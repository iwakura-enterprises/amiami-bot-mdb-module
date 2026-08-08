package enterprises.iwakura.amitracker.service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@RequiredArgsConstructor
public class ConcurrencyService {

    private static final long THROTTLE_MS = 250;
    private static final int QUERY_THREADS = 8;
    private static final int QUERY_QUEUE_CAPACITY = 10_000;

    private final Executor queryExecutor = new ThreadPoolExecutor(
        QUERY_THREADS,
        QUERY_THREADS,
        0L,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(QUERY_QUEUE_CAPACITY),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
    private final Executor commandExecutor = Executors.newCachedThreadPool();
    private final Executor throttledExecutor = Executors.newCachedThreadPool();
    private Executor proxyExecutor = Executors.newFixedThreadPool(1); // Will be replaced

    private final Map<String, ThrottleQueue> throttleQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);

    private final ConfigurationService configurationService;

    public void init() {
        proxyExecutor = Executors.newFixedThreadPool(configurationService.getProxyConfiguration().getProbeThreads());
    }

    public void scheduleQuery(Runnable runnable) {
        queryExecutor.execute(runSafe(runnable, "Query"));
    }

    public void scheduleCommand(Runnable runnable) {
        commandExecutor.execute(runSafe(runnable, "Command"));
    }

    public <T> CompletableFuture<T> scheduleProxy(Supplier<T> supplier) {
        var future = new CompletableFuture<T>();
        proxyExecutor.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public void scheduleThrottled(String key, Runnable runnable) {
        var task = runSafe(runnable, "Throttled");
        var startDrain = new boolean[1];

        throttleQueues.compute(key, (mapKey, throttleQueue) -> {
            var resolvedQueue = throttleQueue == null ? new ThrottleQueue() : throttleQueue;
            resolvedQueue.tasks.add(task);

            // Only start the drainer if one isn't already running for this key
            if (!resolvedQueue.active) {
                resolvedQueue.active = true;
                startDrain[0] = true;
            }

            return resolvedQueue;
        });

        if (startDrain[0]) {
            drainNext(key);
        }
    }

    private void drainNext(String key) {
        var next = new Runnable[1];

        throttleQueues.compute(key, (mapKey, throttleQueue) -> {
            if (throttleQueue == null) {
                return null;
            }

            next[0] = throttleQueue.tasks.poll();

            if (next[0] == null) {
                throttleQueue.active = false;
                return null;
            }

            return throttleQueue;
        });

        if (next[0] == null) {
            return;
        }

        throttledExecutor.execute(next[0]);

        scheduledExecutor.schedule(
            () -> drainNext(key),
            THROTTLE_MS,
            TimeUnit.MILLISECONDS
        );
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

    private static final class ThrottleQueue {

        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private boolean active;
    }
}
