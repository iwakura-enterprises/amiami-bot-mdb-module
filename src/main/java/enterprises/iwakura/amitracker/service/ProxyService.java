package enterprises.iwakura.amitracker.service;

import static enterprises.iwakura.kirara.amiami.AmiAmiApi.DEFAULT_AMIAMI_URL;
import static enterprises.iwakura.kirara.amiami.AmiAmiApi.DEFAULT_API_URL;
import static enterprises.iwakura.kirara.amiami.AmiAmiApi.DEFAULT_IMAGE_API_URL;
import static enterprises.iwakura.kirara.amiami.AmiAmiApi.SUPPORTED_CONTENT_TYPES;

import java.net.Proxy.Type;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;

import enterprises.iwakura.amitracker.constant.ProxyState;
import enterprises.iwakura.amitracker.database.entity.ProxyEntity;
import enterprises.iwakura.amitracker.database.repository.ProxyRepository;
import enterprises.iwakura.amitracker.mapper.ProxyMapper;
import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.amitracker.service.proxy.BaseProxyFetcher;
import enterprises.iwakura.amitracker.service.proxy.SimpleProxyFetcher;
import enterprises.iwakura.amitracker.util.OkHttpProxyHttpCore;
import enterprises.iwakura.cirno.CompletableFutureUtils;
import enterprises.iwakura.kirara.amiami.AmiAmiApi;
import enterprises.iwakura.kirara.amiami.request.AmiAmiItemDetailsRequest;
import enterprises.iwakura.kirara.gson.GsonSerializer;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProxyService {

    /*
    Proxy sites:
     - https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies&proxy_format=ipport&format=json&page=1&skip=2000
     - https://raw.githubusercontent.com/proxifly/free-proxy-list/refs/heads/main/proxies/all/data.json
     - https://raw.githubusercontent.com/monosans/proxy-list/refs/heads/main/proxies_pretty.json

     - https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks5.txt
     - https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks4.txt
     - https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt
     - https://raw.githubusercontent.com/hookzof/socks5_list/master/proxy.txt <- socks5 only
     - https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/http.txt
     - https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/https.txt
     - https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/socks4.txt
     - https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/socks5.txt
     - https://raw.githubusercontent.com/roosterkid/openproxylist/refs/heads/main/HTTPS_RAW.txt
     - https://raw.githubusercontent.com/roosterkid/openproxylist/refs/heads/main/SOCKS4_RAW.txt
     - https://raw.githubusercontent.com/roosterkid/openproxylist/refs/heads/main/SOCKS5_RAW.txt

     - https://proxylist.geonode.com/api/proxy-list?page=1&limit=500&sort_by=responseTime&sort_type=asc <- err: {} pokud error, 401
     - https://www.proxy-list.download/api/v1/get?type=http <- možná nefunguje?
     - https://openproxy.space/list/http <- možná nefunguje?
     - https://free-proxy-list.net/en/ <- http scraping needed
     - https://hide.mn/en/proxy-list/?start=64#list <- http scraping, pagination, requires cookie header, see tomorrow if it works in postman
     - https://www.proxynova.com/proxy-server-list/ <- works w/o headers, but requires javascript runtime
     */

    public static final int CONNECT_TIMEOUT_SECONDS = 10;
    public static final int READ_TIMEOUT_SECONDS = 15;
    private static final AmiAmiItemDetailsRequest PROBE_REQUEST = AmiAmiItemDetailsRequest.builder()
        .gCode("GOODS-00270680")
        .build();

    private final ConcurrencyService concurrencyService;
    private final ProxyRepository proxyRepository;
    private final DatabaseService databaseService;
    private final ConfigurationService configurationService;
    private final Gson gson;

    private final ProxyMapper proxyMapper;

    private final List<BaseProxyFetcher> proxyFetchers; // Sigewine-injected
    private final List<BaseProxyFetcher> allProxyFetchers = new ArrayList<>();

    private GsonSerializer gsonSerializer;
    private OkHttpClient probeHttpClient;

    /**
     * Initializes ProxyService
     */
    public void init() {
        log.info("Initializing ProxyService...");

        gsonSerializer = new GsonSerializer(gson, SUPPORTED_CONTENT_TYPES);
        probeHttpClient = new OkHttpClient.Builder()
            .protocols(List.of(Protocol.HTTP_1_1))
            .connectTimeout(Duration.of(CONNECT_TIMEOUT_SECONDS, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(READ_TIMEOUT_SECONDS, ChronoUnit.SECONDS))
            .build();

        allProxyFetchers.clear();
        allProxyFetchers.addAll(proxyFetchers);
        allProxyFetchers.add(new SimpleProxyFetcher(Type.SOCKS, "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks5.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.SOCKS, "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks4.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.HTTP, "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.SOCKS, "https://raw.githubusercontent.com/hookzof/socks5_list/master/proxy.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.HTTP, "https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/http.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.HTTP, "https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/https.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.SOCKS, "https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/socks4.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.SOCKS, "https://raw.githubusercontent.com/zloi-user/hideip.me/refs/heads/main/socks5.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.HTTP, "https://raw.githubusercontent.com/roosterkid/openproxylist/refs/heads/main/HTTPS_RAW.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.SOCKS, "https://raw.githubusercontent.com/roosterkid/openproxylist/refs/heads/main/SOCKS4_RAW.txt"));
        allProxyFetchers.add(new SimpleProxyFetcher(Type.SOCKS, "https://raw.githubusercontent.com/roosterkid/openproxylist/refs/heads/main/SOCKS5_RAW.txt"));

        log.info("There are {} proxy fetchers", allProxyFetchers.size());
    }

    /**
     * Picks a proxy to use and updates its last used
     *
     * @return ProxyDTO
     */
    public ProxyDTO pick() {
        return proxyRepository.pick()
            .map(proxyMapper::toDTO)
            .orElseGet(() -> {
                log.warn("Could not pick any proxy for request!");
                return ProxyDTO.NO_PROXY;
            });
    }

    /**
     * Fetches and saves proxies into the database
     */
    public void fetchPrexies() {
        var proxyConfig = configurationService.getProxyConfiguration();

        if (!proxyConfig.isProxyFetchEnabled()) {
            return;
        }

        allProxyFetchers.stream()
            .map(fetcher -> Map.entry(fetcher, concurrencyService.scheduleProxy(fetcher::fetch)))
            .forEach(it -> it.getValue().whenCompleteAsync(CompletableFutureUtils.$safe((fetchedProxies, exception) -> {
                if (exception != null) {
                    log.error("Failed to fetch proxies from fetcher {}",
                        it.getKey().toString(), exception
                    );
                    return;
                }

                proxyRepository.getOrInsertAll(fetchedProxies);
                log.info("Fetched {} proxies from fetcher {}",
                    fetchedProxies.size(), it.getKey().toString()
                );
            })));
    }

    /**
     * Finds proxies to probe
     */
    public void probeProxies() {
        var proxyConfig = configurationService.getProxyConfiguration();

        if (!proxyConfig.isProxyProbeEnabled()) {
            return;
        }

        var proxies = proxyRepository.findToProbe(proxyConfig.getMaxPerProbe());

        if (proxies.isEmpty()) {
            return;
        }

        log.info("Found {} proxies to probe (max {})", proxies.size(), proxyConfig.getMaxPerProbe());
        var probeFutures = proxies.stream()
            .map(proxy -> Map.entry(proxy, concurrencyService.scheduleProxy(() -> probeProxy(proxy))))
            .map(it -> it.getValue().handleAsync((proxyState, exception) -> {
                var proxy = it.getKey();
                if (exception != null) {
                    log.error("Uncaught exception during probe of proxy {}", proxy.getId(), exception);
                    proxy.setState(ProxyState.NOT_READY);
                } else if (proxyState == ProxyState.READY) {
                    log.debug("Working proxy {} with score {}", proxy.getId(), proxy.calculateScore());
                }
                return proxy;
            }))
            .toList();

        var updatedProxies = probeFutures.stream()
            .map(CompletableFuture::join)
            .toList();
        log.info("Probed {} proxies, {} of them are READY",
            updatedProxies.size(), updatedProxies.stream().filter(it -> it.getState() == ProxyState.READY).count()
        );

        // saved in a stable id order so this doesn't lock rows in a different order than other
        // concurrent writers (e.g. checkLowQualityProxies), which is what causes deadlocks
        var orderedProxies = updatedProxies.stream()
            .sorted(Comparator.comparing(ProxyEntity::getId))
            .toList();
        proxyRepository.saveAll(orderedProxies);
    }

    /**
     * Checks any low quality proxies
     */
    public void checkLowQualityProxies() {
        var proxyConfig = configurationService.getProxyConfiguration();

        if (!proxyConfig.isProxyFetchEnabled()) {
            return;
        }

        var updatedProxies = proxyRepository.useUpLowScoreProxies(proxyConfig.getUsedUpScore());

        if (updatedProxies != 0) {
            log.info("Used up {} proxies", updatedProxies);
        }
    }

    /**
     * Probes proxy
     *
     * @param proxy Proxy
     *
     * @return Proxy state
     */
    private ProxyState probeProxy(ProxyEntity proxy) {
        var proxyConfig = configurationService.getProxyConfiguration();

        try {
            var api = new AmiAmiApi(
                DEFAULT_API_URL,
                DEFAULT_IMAGE_API_URL,
                DEFAULT_AMIAMI_URL,
                new OkHttpProxyHttpCore(
                    configurationService,
                    this,
                    probeHttpClient,
                    proxyMapper.toDTO(proxy)
                ),
                gsonSerializer
            );

            var start = System.currentTimeMillis();
            var response = api.getItemDetails(PROBE_REQUEST).send().join();
            proxy.setLatencyMillis((int) (System.currentTimeMillis() - start));

            if (response != null) {
                if (response.isSuccessful()) {
                    proxy.setState(ProxyState.READY);
                } else {
                    proxy.setState(ProxyState.NOT_READY);
                    proxy.setLastError(response.getResponseMessage());
                }
            } else {
                proxy.setState(ProxyState.NOT_READY);
                proxy.setLastError("Response null");
            }
        } catch (Exception exception) {
            proxy.setLastError(exception.toString());
            proxy.setState(ProxyState.NOT_READY);
        }

        if (proxy.getState() == ProxyState.READY) {
            proxy.addTimesAlive();
        } else {
            proxy.addTimesDead();
        }

        // Tried at least 10 times
        if ((proxy.getReliabilityRatio() <= proxyConfig.getUsedUpReliabilityRatio() && proxy.getTimesDead() > 10)
            // Or dead n times in a row
            || proxy.getTimesDeadInRow() >= proxyConfig.getMaxTimesDeadInRow()
        ) {
            log.info("Proxy {} has been used up", proxy.getId());
            proxy.setState(ProxyState.USED_UP);
            proxy.setLastError("(Used Up Reliability Reached) " + Optional.ofNullable(proxy.getLastError()).orElse(""));
        }

        proxy.setLastUsedAt(OffsetDateTime.now());
        return proxy.getState();
    }

    /**
     * Schedules update of the specified proxies
     *
     * @param proxies Proxies
     */
    public void scheduleUpdateProxies(List<ProxyDTO> proxies) {
        var proxyConfig = configurationService.getProxyConfiguration();

        concurrencyService.scheduleProxy(() -> {
            databaseService.runInThreadTransaction(session -> {
                for (ProxyDTO proxy : proxies) {
                    if (proxy.getTimesDeadInRow() >= proxyConfig.getMaxTimesDeadInRow()) {
                        proxy.setState(ProxyState.NOT_READY);
                    }

                    proxyRepository.findById(proxy.getId())
                        .map(entity -> proxyMapper.update(entity, proxy))
                        .ifPresent(proxyRepository::save);
                }
            });
            return null;
        });
    }
}