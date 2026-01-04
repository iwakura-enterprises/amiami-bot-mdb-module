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

    private final Executor queryExecutor = Executors.newFixedThreadPool(8);
    private final Executor commandExecutor = Executors.newCachedThreadPool();

    public void scheduleQuery(Runnable runnable) {
        queryExecutor.execute(runnable);
    }

    public void scheduleCommand(Runnable runnable) {
        commandExecutor.execute(runnable);
    }
}
