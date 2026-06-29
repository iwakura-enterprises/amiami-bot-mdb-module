package enterprises.iwakura.amitracker.database.repository;

import java.net.Proxy.Type;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.ProxyEntity;
import enterprises.iwakura.amitracker.mapper.ProxyMapper;
import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
public class ProxyRepository extends AmiBaseRepository<ProxyEntity, Long> {

    private final ProxyMapper proxyMapper;

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ProxyRepository(
        DatabaseService databaseService,
        ProxyMapper proxyMapper
    ) {
        super(databaseService);
        this.proxyMapper = proxyMapper;
    }

    @Override
    public Class<ProxyEntity> getEntityClass() {
        return ProxyEntity.class;
    }

    @Override
    protected boolean hasId(ProxyEntity proxyEntity) {
        return proxyEntity.getId() != null;
    }

    public Optional<ProxyEntity> getByProtocolIpAndPort(Type protocol, String ip, int port) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      SELECT p
                      FROM ProxyEntity p
                      WHERE p.protocol = :protocol AND p.ip = :ip AND p.port = :port
                      """;
            return session.createQuery(hql, ProxyEntity.class)
                .setParameter("protocol", protocol)
                .setParameter("ip", ip)
                .setParameter("port", port)
                .uniqueResultOptional();
        });
    }

    /**
     * Finds proxies to probe
     *
     * @param maxPerProbe Maximum number of proxies to probe
     *
     * @return List of proxies
     */
    public List<ProxyEntity> findToProbe(int maxPerProbe) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      SELECT p
                      FROM ProxyEntity p
                      WHERE p.state = ProxyState.NOT_READY
                      ORDER BY p.score DESC, p.timesDead ASC
                      LIMIT :maxPerProbe
                      """;
            return session.createQuery(hql, ProxyEntity.class)
                .setParameter("maxPerProbe", maxPerProbe)
                .getResultList();
        });
    }

    /**
     * Gets, updates or inserts all specified proxies
     *
     * @param proxies Proxies
     *
     * @return List of ProxyEntity
     */
    public List<ProxyEntity> getOrInsertAll(List<ProxyDTO> proxies) {
        var sorted = proxies.stream()
            .sorted(Comparator.comparing((ProxyDTO p) -> p.getProtocol().name())
                .thenComparing(ProxyDTO::getIp)
                .thenComparingInt(ProxyDTO::getPort))
            .toList();

        return databaseService.runInThreadTransaction(session -> {
            return sorted.stream().map(proxy ->
                getByProtocolIpAndPort(proxy.getProtocol(), proxy.getIp(), proxy.getPort())
                    .map(entity -> save(proxyMapper.update(entity, proxy)))
                    .orElseGet(() -> save(proxyMapper.create(proxy)))
            ).toList();
        });
    }

    /**
     * Picks the best candidate for sending requests
     *
     * @return Optional of ProxyEntity
     */
    public Optional<ProxyEntity> pick() {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      SELECT p
                      FROM ProxyEntity p
                      WHERE p.state = ProxyState.READY
                      ORDER BY p.lastUsedAt ASC
                      LIMIT 10
                      """;
            var candidates = session.createQuery(hql, ProxyEntity.class)
                .getResultList();
            return candidates.stream()
                .max(Comparator.comparingDouble(ProxyEntity::getScore))
                .map(proxy -> {
                    proxy.setLastUsedAt(OffsetDateTime.now());
                    return save(proxy);
                });
        });
    }
}
