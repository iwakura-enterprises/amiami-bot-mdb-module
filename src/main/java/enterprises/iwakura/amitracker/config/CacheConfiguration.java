package enterprises.iwakura.amitracker.config;

import lombok.Data;

@Data
public class CacheConfiguration {

    private long maxProductCacheSize = 100_000;
    private long maxProductListCacheSize = 50_000;
    private long maxImageUrlCacheSize = 100_000;
    private long imageUrlCacheExpireMillis = 86400000; // 24 hours
}
