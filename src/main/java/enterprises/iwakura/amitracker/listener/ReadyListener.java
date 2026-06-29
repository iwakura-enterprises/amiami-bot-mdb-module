package enterprises.iwakura.amitracker.listener;

import java.util.List;

import enterprises.iwakura.amitracker.service.scheduler.DiscordBaseScheduler;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Slf4j
@Bean
@RequiredArgsConstructor
public class ReadyListener extends ListenerAdapter {

    private final List<DiscordBaseScheduler> baseSchedulers;

    @Override
    public void onGenericEvent(GenericEvent event) {
        if (event instanceof ReadyEvent readyEvent) {
            log.info("JDA finished loading, guilds: {} (available: {}, unavailable: {})",
                readyEvent.getGuildTotalCount(),
                readyEvent.getGuildAvailableCount(),
                readyEvent.getGuildUnavailableCount()
            );

            log.info("Starting Discord-related schedulers...");
            baseSchedulers.forEach(DiscordBaseScheduler::initialize);
        }
    }
}
