package enterprises.iwakura.amitracker.config;

import lombok.Data;

@Data
public class ProductQueryConfiguration {

    private long searchQueryIntervalMillis = 60000; // 1 minute
    private long itemDetailQueryIntervalMillis = 300000; // 5 minutes
    private int apiQueryThreads = 1;
    private long apiQueryMinIntervalMillis = 250; // 0.25 seconds
}
