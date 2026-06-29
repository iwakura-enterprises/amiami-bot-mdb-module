package enterprises.iwakura.amitracker.service.scheduler;

import java.util.concurrent.TimeUnit;

import enterprises.iwakura.amitracker.service.ProductChangeAnnounceService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductChangeAnnouncementScheduler extends DiscordBaseScheduler {

    private final ProductChangeAnnounceService productChangeAnnounceService;

    @Override
    public void initialize() {
        log.info("Initializing ProductAnnouncementScheduler...");
        this.schedule("SendQueuedProductChangeAnnouncements", 0, 10, TimeUnit.SECONDS, productChangeAnnounceService::sendQueuedProductChangeAnnouncements);
    }
}
