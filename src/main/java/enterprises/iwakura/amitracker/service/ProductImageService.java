package enterprises.iwakura.amitracker.service;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import enterprises.iwakura.amitracker.database.repository.ProductRepository;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductImageService {

    // TODO: Add TTL for images on Akasha (like 1 year)

    public static final String DEFAULT_IMAGE_URL = "https://akasha.iwakura.enterprises/data-source/hetzner/public/amitracker/product/404.png";

    private final ConcurrencyService concurrencyService;
    private final ConfigurationService configurationService;
    private final AmiAmiQueryService amiAmiQueryService;
    private final AkashaApiService akashaApiService;
    private final ProductRepository productRepository;

    private Cache<String, String> imageUrlCache;

    /**
     * Initializes the ProductImageService by setting up the image URL cache.
     */
    public void init() {
        log.info("Initializing ProductImageService...");
        var cacheConfiguration = configurationService.getCache();

        imageUrlCache = CacheBuilder.newBuilder()
            .maximumSize(cacheConfiguration.getMaxProductCacheSize())
            .expireAfterWrite(cacheConfiguration.getImageUrlCacheExpireMillis(), TimeUnit.MILLISECONDS)
            .recordStats()
            .build();
    }

    /**
     * Fetches the image URL for the given image URL. If the image URL is cached, it returns the cached value.
     * Otherwise, it fetches the image data from the AmiAmiQueryService, uploads it to Akasha, and caches the result.
     *
     * @param imageUrl the image URL to fetch (gotten from AmiAmi API)
     *
     * @return the fetched or cached image URL that can be used in Discord embeds and elsewhere
     */
    @SneakyThrows
    public CompletableFuture<String> fetchImageUrl(String imageUrl) {
        var productCode = getProductCodeFromUrl(imageUrl);

        var cachedImageUrl = imageUrlCache.getIfPresent(productCode);
        if (cachedImageUrl != null) {
            return CompletableFuture.completedFuture(cachedImageUrl);
        }

        var future = new CompletableFuture<String>();

        concurrencyService.scheduleAkashaImageFetch(() -> {
            try {
                var laterCachedImageUrl = imageUrlCache.getIfPresent(productCode);

                if (laterCachedImageUrl != null) {
                    // Another thread has cached the image while waiting for the scheduled task
                    future.complete(laterCachedImageUrl);
                    return;
                }

                String akashaImageUrl = null;

                // Image not in cache, check if product's image has been updated before
                if (productRepository.hasLastImageUpdate(productCode)) {
                    // Try to get it from Akasha
                    try {
                        var response = akashaApiService.existsProductImage(productCode).join();
                        if (response == null || !response) {
                            throw new Exception("Image not found on Akasha");
                        }

                        // Image exists on Akasha, cache and return the URL
                        akashaImageUrl = akashaApiService.constructImageUrl(productCode);
                        log.info("Created image URL for product code {} from Akasha: {}", productCode, akashaImageUrl);
                    } catch (Exception exception) {
                        // Failed to get from Akasha, fetch and cache again
                    }
                }

                if (akashaImageUrl == null) {
                    // Never downloaded / not found in Akasha, fetch from AmiAmi and upload
                    var imageData = amiAmiQueryService.queryImage(imageUrl).join();
                    akashaImageUrl = akashaApiService.writeProductImage(productCode, imageData).join();
                    log.info("Fetched and uploaded image for product code {} to Akasha: {}", productCode, akashaImageUrl);
                    productRepository.updateLastImageUpdate(productCode, OffsetDateTime.now());
                }

                imageUrlCache.put(productCode, akashaImageUrl);
                future.complete(akashaImageUrl);
            } catch (Exception exception) {
                log.error("Failed to fetch image URL for product code {}", productCode, exception);
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    /**
     * Extracts the product code from the given image URL.
     *
     * @param imageUrl the image URL
     *
     * @return the extracted product code
     *
     * @throws IllegalArgumentException if the image URL format is invalid
     */
    private String getProductCodeFromUrl(String imageUrl) {
        // Example URL: "/images/product/main/253/FIGURE-188668.jpg"
        // => FIGURE-188668
        var lastSlashIndex = imageUrl.lastIndexOf('/');
        var lastDotIndex = imageUrl.lastIndexOf('.');
        if (lastSlashIndex == -1 || lastDotIndex == -1 || lastDotIndex <= lastSlashIndex) {
            throw new IllegalArgumentException("Invalid image URL format: " + imageUrl);
        }
        return imageUrl.substring(lastSlashIndex + 1, lastDotIndex);
    }
}
