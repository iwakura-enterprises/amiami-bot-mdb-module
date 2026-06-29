package enterprises.iwakura.amitracker.service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@RequiredArgsConstructor
public class ConcurrencyService {

    private static final long THROTTLE_MS = 250;

    private final Executor queryExecutor = Executors.newFixedThreadPool(8);
    private final Executor commandExecutor = Executors.newCachedThreadPool();
    private final Executor throttledExecutor = Executors.newCachedThreadPool();
    private Executor proxyExecutor = Executors.newFixedThreadPool(1); // Will be replaced

    private final Map<String, Queue<Runnable>> throttleQueues = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> throttleActive = new ConcurrentHashMap<>();
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
        Queue<Runnable> queue = throttleQueues.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        AtomicBoolean active = throttleActive.computeIfAbsent(key, k -> new AtomicBoolean(false));

        queue.add(runSafe(runnable, "Throttled"));

        // Only start the drainer if one isn't already running for this key
        if (active.compareAndSet(false, true)) {
            drainNext(key, queue, active);
        }
    }

    private void drainNext(String key, Queue<Runnable> queue, AtomicBoolean active) {
        Runnable next = queue.poll();
        if (next == null) {
            active.set(false);
            if (!queue.isEmpty() && active.compareAndSet(false, true)) {
                drainNext(key, queue, active);
            }
            return;
        }

        throttledExecutor.execute(next);

        scheduledExecutor.schedule(
            () -> drainNext(key, queue, active),
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
}
