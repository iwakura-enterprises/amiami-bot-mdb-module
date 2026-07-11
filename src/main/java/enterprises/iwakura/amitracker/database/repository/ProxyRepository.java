package enterprises.iwakura.amitracker.database.repository;

import java.net.Proxy.Type;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import enterprises.iwakura.amitracker.database.entity.ProxyEntity;
import enterprises.iwakura.amitracker.mapper.ProxyMapper;
import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
public class ProxyRepository extends AmiBaseRepository<ProxyEntity, Long> {

    /**
     * Size of the score-ranked pool weighted random selection is drawn from
     */
    private static final int PICK_POOL_SIZE = 100;

    /**
     * Proxies used more recently than this are skipped, so the same proxy isn't hammered back-to-back
     */
    private static final int PICK_COOLDOWN_SECONDS = 30;

    /**
     * Controls how strongly high scores are preferred over low ones
     */
    private static final double SCORE_SHARPNESS = 3.0;

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

    @Override
    public ProxyEntity save(ProxyEntity entity) {
        if (entity.getId() != null) {
            return super.save(entity);
        }

        return databaseService.runInThreadTransaction(session -> {
            var sql = """
                      INSERT INTO proxy (state, protocol, ip, port, score, latencyMillis, timesAlive, timesDead,
                                        lastUsedAt, countyCode, anonymityLevel, lastError, responseData)
                      VALUES (:state, :protocol, :ip, :port, :score, :latencyMillis, :timesAlive, :timesDead,
                              :lastUsedAt, :countyCode, :anonymityLevel, :lastError, :responseData)
                      ON CONFLICT (protocol, ip, port) DO NOTHING
                      """;
            session.createNativeQuery(sql)
                .setParameter("state", entity.getState().name())
                .setParameter("protocol", entity.getProtocol().name())
                .setParameter("ip", entity.getIp())
                .setParameter("port", entity.getPort())
                .setParameter("score", entity.getScore())
                .setParameter("latencyMillis", entity.getLatencyMillis())
                .setParameter("timesAlive", entity.getTimesAlive())
                .setParameter("timesDead", entity.getTimesDead())
                .setParameter("lastUsedAt", entity.getLastUsedAt())
                .setParameter("countyCode", entity.getCountyCode())
                .setParameter("anonymityLevel", entity.getAnonymityLevel() != null ? entity.getAnonymityLevel().name() : null)
                .setParameter("lastError", entity.getLastError())
                .setParameter("responseData", entity.getResponseData())
                .executeUpdate();

            var hql = """
                      SELECT p FROM ProxyEntity p
                      WHERE p.protocol = :protocol AND p.ip = :ip AND p.port = :port
                      """;
            return session.createQuery(hql, ProxyEntity.class)
                .setParameter("protocol", entity.getProtocol())
                .setParameter("ip", entity.getIp())
                .setParameter("port", entity.getPort())
                .uniqueResult();
        });
    }

    /**
     * Picks a proxy candidate using weighted random selection within a score-ranked pool
     */
    public Optional<ProxyEntity> pick() {
        return databaseService.runInThreadTransaction(session -> {
            var candidates = findPickCandidates(session, OffsetDateTime.now().minusSeconds(PICK_COOLDOWN_SECONDS));
            if (candidates.isEmpty()) {
                candidates = findPickCandidates(session, null);
            }
            if (candidates.isEmpty()) {
                return Optional.empty();
            }

            var selected = weightedPick(candidates);
            selected.setLastUsedAt(OffsetDateTime.now());
            return Optional.of(save(selected));
        });
    }

    private List<ProxyEntity> findPickCandidates(Session session, OffsetDateTime cooldownCutoff) {
        if (cooldownCutoff != null) {
            var hql = """
                  SELECT p
                  FROM ProxyEntity p
                  WHERE p.state = ProxyState.READY AND p.lastUsedAt <= :cooldownCutoff
                  ORDER BY p.score DESC
                  LIMIT :poolSize
                  """;
            return session.createQuery(hql, ProxyEntity.class)
                .setParameter("cooldownCutoff", cooldownCutoff)
                .setParameter("poolSize", PICK_POOL_SIZE)
                .getResultList();
        } else {
            var hql = """
                  SELECT p
                  FROM ProxyEntity p
                  WHERE p.state = ProxyState.READY
                  ORDER BY p.score DESC
                  LIMIT :poolSize
                  """;
            return session.createQuery(hql, ProxyEntity.class)
                .setParameter("poolSize", PICK_POOL_SIZE)
                .getResultList();
        }
    }

    private ProxyEntity weightedPick(List<ProxyEntity> candidates) {
        double totalWeight = candidates.stream()
            .mapToDouble(p -> Math.exp(p.getScore() * SCORE_SHARPNESS))
            .sum();

        double spin = Math.random() * totalWeight;
        double cumulative = 0;
        for (var proxy : candidates) {
            cumulative += Math.exp(proxy.getScore() * SCORE_SHARPNESS);
            if (cumulative >= spin) {
                return proxy;
            }
        }

        return candidates.getLast();
    }

    /**
     * Marks all READY proxies scoring below the given threshold as USED_UP so they stop being picked.
     *
     * @param score Score threshold; proxies with a lower score are marked USED_UP
     */
    public int useUpLowScoreProxies(double score) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      UPDATE ProxyEntity p
                      SET p.state = ProxyState.USED_UP
                      WHERE p.state = ProxyState.READY AND p.score < :score
                      """;
            return session.createMutationQuery(hql)
                .setParameter("score", score)
                .executeUpdate();
        });
    }
}
