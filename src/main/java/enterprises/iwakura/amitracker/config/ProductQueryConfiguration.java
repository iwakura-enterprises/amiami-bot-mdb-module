package enterprises.iwakura.amitracker.config;

import lombok.Data;

@Data
public class ProductQueryConfiguration {

    private long searchQueryIntervalMillis = 60000; // 1 minute
    private long itemDetailQueryIntervalMillis = 300000; // 5 minutes
    private int apiQueryThreads = 2;
    private long queryBackoffMillis = 250; // 0.25 seconds
    private long rateLimitBackoffMillis = 30_000; // 30 seconds
    private long maxQueryRetriesOnRateLimit = 5;

    private int noImageMaxRetryCount = 7;
    private long noImageRefreshBackOffBase = 300000; // 5 minutes
}
