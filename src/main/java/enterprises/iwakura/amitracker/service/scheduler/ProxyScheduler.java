package enterprises.iwakura.amitracker.service.scheduler;

import java.util.concurrent.TimeUnit;

import enterprises.iwakura.amitracker.service.ProxyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProxyScheduler extends BaseScheduler {

    private final ProxyService proxyService;

    @Override
    public void initialize() {
        log.info("Initializing ProxyScheduler...");
        this.schedule("Fetch", 0, 30, TimeUnit.MINUTES, proxyService::fetchPrexies);
        this.schedule("Probe", 0, 1, TimeUnit.MINUTES, proxyService::probeProxies);
    }
}
