package enterprises.iwakura.amitracker.service;

import java.nio.file.Path;

import com.google.gson.Gson;

import enterprises.iwakura.amitracker.config.CacheConfiguration;
import enterprises.iwakura.amitracker.config.ProductChangeAnnouncementConfiguration;
import enterprises.iwakura.amitracker.config.ProductQueryConfiguration;
import enterprises.iwakura.amitracker.config.ProxyConfiguration;
import enterprises.iwakura.irminsul.DatabaseServiceConfiguration;
import enterprises.iwakura.jean.Jean;
import enterprises.iwakura.jean.LoadOptions;
import enterprises.iwakura.jean.serializer.GsonSerializer;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ConfigurationService {

    private final Gson gson;
    private Jean jean;

    /**
     * Initializes the ConfigurationService with the specified module directory path.
     *
     * @param moduleDirectoryPath the path to the module's directory
     */
    public void init(Path moduleDirectoryPath) {
        log.info("Initializing ConfigurationService with directory: {}", moduleDirectoryPath.toAbsolutePath());
        jean = new Jean(
            moduleDirectoryPath,
            new GsonSerializer(gson),
            LoadOptions.builder().saveOnLoad(true).build()
        );
    }

    /**
     * Gets the database configuration.
     *
     * @return the database configuration
     */
    public DatabaseServiceConfiguration getDatabase() {
        return jean.getOrLoad("database", DatabaseServiceConfiguration.class);
    }

    /**
     * Gets the product query configuration.
     *
     * @return the product query configuration
     */
    public ProductQueryConfiguration getProductQuery() {
        return jean.getOrLoad("product_query", ProductQueryConfiguration.class);
    }

    /**
     * Gets the cache configuration.
     *
     * @return the cache configuration
     */
    public CacheConfiguration getCache() {
        return jean.getOrLoad("cache", CacheConfiguration.class);
    }

    /**
     * Gets the ProductChangeAnnouncementConfiguration.
     *
     * @return the ProductChangeAnnouncementConfiguration
     */
    public ProductChangeAnnouncementConfiguration getProductChangeAnnouncementConfiguration() {
        return jean.getOrLoad("product-change-announcement", ProductChangeAnnouncementConfiguration.class);
    }

    /**
     * Gets the ProxyConfiguration
     *
     * @return ProxyConfiguration
     */
    public ProxyConfiguration getProxyConfiguration() {
        return jean.getOrLoad("proxy", ProxyConfiguration.class);
    }
}
