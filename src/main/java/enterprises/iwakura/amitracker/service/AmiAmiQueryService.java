package enterprises.iwakura.amitracker.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import enterprises.iwakura.amitracker.exception.QueryFailedException;
import enterprises.iwakura.amitracker.objects.query.ProductListQueryRequest;
import enterprises.iwakura.amitracker.objects.query.ProductQueryRequest;
import enterprises.iwakura.kirara.amiami.request.AmiAmiItemDetailsRequest;
import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiSearchResponse;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class AmiAmiQueryService {

    private final ConfigurationService configurationService;
    private final AmiAmiApiService amiAmiApiService;

    private Cache<String, AmiAmiItemResponse> itemResponseCache;
    private Cache<Long, AmiAmiSearchResponse> itemSearchResponseCache;
    private Executor executor;
    private long lastQueryTimeMillis = 0L;

    /**
     * Initializes the AmiAmiQueryService.
     */
    public void init() {
        log.info("Initializing AmiAmiQueryService...");
        var productQueryConfiguration = configurationService.getProductQuery();
        var cacheConfiguration = configurationService.getCache();

        executor = Executors.newFixedThreadPool(productQueryConfiguration.getApiQueryThreads());

        itemResponseCache = CacheBuilder.newBuilder()
            .maximumSize(cacheConfiguration.getMaxProductCacheSize())
            .expireAfterWrite((long) (productQueryConfiguration.getItemDetailQueryIntervalMillis() * 0.9), TimeUnit.MILLISECONDS)
            .recordStats()
            .build();

        itemSearchResponseCache = CacheBuilder.newBuilder()
            .maximumSize(cacheConfiguration.getMaxProductListCacheSize())
            .expireAfterWrite((long) (productQueryConfiguration.getSearchQueryIntervalMillis() * 0.9), TimeUnit.MILLISECONDS)
            .recordStats()
            .build();
    }

    /**
     * Schedules a search query to be performed in the background.
     *
     * @param queryRequest The search query request details.
     *
     * @return A CompletableFuture that will complete with the search response.
     */
    public CompletableFuture<AmiAmiSearchResponse> scheduleSearch(ProductListQueryRequest queryRequest) {
        var future = new CompletableFuture<AmiAmiSearchResponse>();

        executor.execute(() -> {
            // Check if recently queried
            var recentResponse = itemSearchResponseCache.getIfPresent(queryRequest.getProductListQueryId());
            if (recentResponse != null) {
                log.warn("ProductListQueryEntity {} found in recently queried cache, skipping query",
                    queryRequest.getProductListQueryId());
                future.complete(recentResponse);
            } else {
                try {
                    var response = performQuery(() -> amiAmiApiService.search(queryRequest.getAmiAmiSearchRequest()).send().join());
                    future.complete(response);
                } catch (Exception exception) {
                    future.completeExceptionally(
                        new QueryFailedException("Failed to query search for ProductListQueryEntity[%d]".formatted(
                            queryRequest.getProductListQueryId()), exception)
                    );
                }
            }
        });

        return future;
    }

    /**
     * Schedules an item detail query to be performed in the background.
     *
     * @param queryRequest The item detail query request details.
     *
     * @return A CompletableFuture that will complete with the item detail response.
     */
    public CompletableFuture<AmiAmiItemResponse> scheduleItemDetail(ProductQueryRequest queryRequest) {
        var future = new CompletableFuture<AmiAmiItemResponse>();

        executor.execute(() -> {
            // Check if recently queried
            var recentResponse = itemResponseCache.getIfPresent(queryRequest.getProductCode());
            if (recentResponse != null) {
                log.warn("Product item {} found in recently queried cache, skipping query",
                    queryRequest.getProductCode());
                future.complete(recentResponse);
            } else {
                var itemDetailsRequest = AmiAmiItemDetailsRequest.builder()
                    .gCode(queryRequest.getProductCode())
                    .build();

                try {
                    var response = performQuery(() ->
                        amiAmiApiService.getItemDetails(itemDetailsRequest).send().join()
                    );
                    itemResponseCache.put(queryRequest.getProductCode(), response);
                    future.complete(response);
                } catch (Exception exception) {
                    future.completeExceptionally(
                        new QueryFailedException("Failed to query item details for product code %s".formatted(
                            queryRequest.getProductCode()), exception)
                    );
                }
            }
        });

        return future;
    }

    /**
     * Schedules an image query to be performed in the background.
     *
     * @param imageUrl The URL of the image to query.
     *
     * @return A CompletableFuture that will complete with the image data as a byte array.
     */
    public CompletableFuture<byte[]> queryImage(String imageUrl) {
        var future = new CompletableFuture<byte[]>();

        executor.execute(() -> {
            try {
                var imageData = performNonAmiResponseQuery(() ->
                    amiAmiApiService.getImage(imageUrl).send().join()
                );
                future.complete(imageData);
            } catch (Exception exception) {
                future.completeExceptionally(
                    new QueryFailedException("Failed to query image from URL %s".formatted(
                        imageUrl), exception)
                );
            }
        });

        return future;
    }

    /**
     * Performs the query with rate limit handling. There can be only one query at a time.
     *
     * @param supplier The supplier that performs the query.
     * @param <T>      The type of the response.
     *
     * @return The response from the query.
     */
    private synchronized <T extends AmiAmiResponse> T performQuery(Supplier<T> supplier) {
        var configuration = configurationService.getProductQuery();
        int retry = 0;
        final long maxRetries = configuration.getMaxQueryRetriesOnRateLimit();

        while (retry < maxRetries) {
            enforceQueryInterval();

            T response;

            try {
                response = supplier.get();
            } catch (Exception exception) {
                throw new QueryFailedException("Failed to query AmiAmi API", exception);
            } finally {
                lastQueryTimeMillis = System.currentTimeMillis();
            }

            if (response.isRateLimited()) {
                long backoffMillis = configuration.getRateLimitBackoffMillis() * (retry + 1);
                log.warn("AmiAmi API rate limited the request! (retry {}/{}) backing off for {}ms",
                    retry + 1, maxRetries, backoffMillis
                );

                sleep(backoffMillis);
            } else {
                if (!response.isSuccessful()) {
                    throw new QueryFailedException("AmiAmi API query failed: " + response.getResponseValue(), null);
                }

                return response;
            }

            retry++;
        }

        throw new QueryFailedException(
            "Exceeded maximum retries (" + maxRetries + ") for AmiAmi API queries due to rate limiting", null);
    }

    /**
     * Performs the query with rate limit handling for non-AmiAmiResponse types. There can be only one query at a time.
     *
     * @param supplier The supplier that performs the query.
     * @param <T>      The type of the response.
     *
     * @return The response from the query.
     */
    private synchronized <T> T performNonAmiResponseQuery(Supplier<T> supplier) {
        var configuration = configurationService.getProductQuery();
        int retry = 0;
        final long maxRetries = configuration.getMaxQueryRetriesOnRateLimit();

        while (retry < maxRetries) {
            enforceQueryInterval();

            T response;

            try {
                response = supplier.get();
            } catch (Exception exception) {
                throw new QueryFailedException("Failed to query AmiAmi API", exception);
            } finally {
                lastQueryTimeMillis = System.currentTimeMillis();
            }

            if (response == null) {
                long backoffMillis = configuration.getRateLimitBackoffMillis() * (retry + 1);
                log.warn("AmiAmi Image API returned null! (retry {}/{}) backing off for {}ms",
                    retry + 1, maxRetries, backoffMillis
                );

                sleep(backoffMillis);
            } else {
                return response;
            }

            retry++;
        }

        throw new QueryFailedException(
            "Exceeded maximum retries (" + maxRetries + ") for AmiAmi API queries due to rate limiting", null);
    }

    /**
     * Enforces the query interval by sleeping if necessary.
     */
    private void enforceQueryInterval() {
        var config = configurationService.getProductQuery();
        long now = System.currentTimeMillis();
        long timeSinceLastQuery = now - lastQueryTimeMillis;
        if (timeSinceLastQuery < config.getQueryBackoffMillis()) {
            sleep(config.getQueryBackoffMillis() - timeSinceLastQuery);
        }
    }

    /**
     * Sleeps for the specified number of milliseconds.
     *
     * @param millis The number of milliseconds to sleep.
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
