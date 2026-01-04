package enterprises.iwakura.amitracker.service.scheduler;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseScheduler {

    protected final Timer timer = new Timer();

    /**
     * Schedules a recurring task with the specified parameters.
     *
     * @param name     the name of the task
     * @param delay    the initial delay before the task is executed
     * @param period   the period between successive executions of the task
     * @param timeUnit the time unit for the delay and period
     * @param task     the task to be executed
     */
    protected void schedule(String name, long delay, long period, TimeUnit timeUnit, Runnable task) {
        long delayMs = timeUnit.toMillis(delay);
        long periodMs = timeUnit.toMillis(period);
        log.info("Scheduling '{}' to run every {} {} after an initial delay of {} {}",
            name, period, timeUnit, delay, timeUnit);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("Error occurred while executing scheduled task '{}': {}", name, e.getMessage(), e);
                }
            }
        }, delayMs, periodMs);
    }

    /**
     * Initializes the scheduler.
     */
    public abstract void initialize();

}
