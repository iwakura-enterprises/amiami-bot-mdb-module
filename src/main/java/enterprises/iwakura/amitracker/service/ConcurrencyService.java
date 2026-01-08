package enterprises.iwakura.amitracker.service;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@RequiredArgsConstructor
public class ConcurrencyService {

    private final Executor akashaImageFetchExecutor = Executors.newCachedThreadPool();
    private final Executor queryExecutor = Executors.newFixedThreadPool(8);
    private final Executor commandExecutor = Executors.newCachedThreadPool();

    public void scheduleAkashaImageFetch(Runnable runnable) {
        akashaImageFetchExecutor.execute(runSafe(runnable, "AkashaImageFetch"));
    }

    public void scheduleQuery(Runnable runnable) {
        queryExecutor.execute(runSafe(runnable, "Query"));
    }

    public void scheduleCommand(Runnable runnable) {
        commandExecutor.execute(runSafe(runnable, "Command"));
    }

    private Runnable runSafe(Runnable runnable, String error) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled exception in {} executor", error, e);
            }
        };
    }
}
