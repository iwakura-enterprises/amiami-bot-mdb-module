package enterprises.iwakura.amitracker.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import enterprises.iwakura.amitracker.database.repository.ProductListQueryRepository;
import enterprises.iwakura.amitracker.database.repository.ProductRepository;
import enterprises.iwakura.amitracker.exception.MissingEntityException;
import enterprises.iwakura.amitracker.exception.QueryException;
import enterprises.iwakura.amitracker.exception.RecentlyQueriedException;
import enterprises.iwakura.amitracker.objects.query.ProductListQueryRequest;
import enterprises.iwakura.amitracker.objects.query.ProductQueryRequest;
import enterprises.iwakura.kirara.amiami.request.AmiAmiItemDetailsRequest;
import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse;
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

    private final ProductListQueryRepository productListQueryRepository;
    private final ProductRepository productRepository;

    private Executor executor;
    private long lastQueryTimeMillis = 0;

    /**
     * Initializes the AmiAmiQueryService.
     */
    public void init() {
        log.info("Initializing AmiAmiQueryService...");
        executor = Executors.newFixedThreadPool(configurationService.getProductQuery().getApiQueryThreads());
    }

    /**
     * Schedules a search query to be performed in the background.
     *
     * @param productListQueryRequest The search query request details.
     * @return A CompletableFuture that will complete with the search response.
     */
    public CompletableFuture<AmiAmiSearchResponse> scheduleSearch(ProductListQueryRequest productListQueryRequest) {
        var future = new CompletableFuture<AmiAmiSearchResponse>();

        executor.execute(() -> {
            // Check if exists
            var optionalProductListQuery = productListQueryRepository.findById(productListQueryRequest.getProductListQueryId());
            if (optionalProductListQuery.isEmpty()) {
                log.warn("ProductListQueryEntity with ID {} not found for query request",
                    productListQueryRequest.getProductListQueryId()
                );
                future.completeExceptionally(new MissingEntityException("ProductListQueryEntity[%d] not found".formatted(
                    productListQueryRequest.getProductListQueryId()
                )));
                return;
            }

            // Enforce minimum interval between API queries for the thread pool
            enforceQueryInterval();
            // TODO: Enforce exponential backoff on rate limiting

            var requestParams = optionalProductListQuery.get().toAmiAmiSearchRequest(productListQueryRequest.getPage());

            try {
                future.complete(amiAmiApiService.search(requestParams).send().join());
            } catch (Exception e) {
                future.completeExceptionally(new QueryException("Error performing AmiAmi search query for ProductListQueryRequest: %s".formatted(
                    productListQueryRequest
                ), e));
            } finally {
                lastQueryTimeMillis = System.currentTimeMillis();
            }
        });

        return future;
    }

    /**
     * Schedules an item detail query to be performed in the background.
     *
     * @param productQueryRequest The item detail query request details.
     * @return A CompletableFuture that will complete with the item detail response.
     */
    public CompletableFuture<AmiAmiItemResponse> scheduleItemDetail(ProductQueryRequest productQueryRequest) {
        var future = new CompletableFuture<AmiAmiItemResponse>();

        executor.execute(() -> {
            var config = configurationService.getProductQuery();

            // Check if recently queried / updated by other means
            var optionalProduct = productRepository.findByCode(productQueryRequest.getProductCode());
            if (optionalProduct.isPresent()) {
                var lastUpdatedMillis = Optional.ofNullable(optionalProduct.get().getUpdatedAt())
                    .map(ts -> ts.toInstant().toEpochMilli())
                    .orElse(0L);
                long now = System.currentTimeMillis();
                if (now - lastUpdatedMillis < config.getItemDetailQueryIntervalMillis()) {
                    log.warn("Skipping item detail query for product code {} as it was recently updated - last updated at {}",
                        productQueryRequest.getProductCode(), optionalProduct.get().getUpdatedAt()
                    );
                    future.completeExceptionally(new RecentlyQueriedException("Product code %s was recently updated".formatted(
                        productQueryRequest.getProductCode()
                    )));
                    return;
                }
            }

            // Enforce minimum interval between API queries for the thread pool
            enforceQueryInterval();
            // TODO: Enforce exponential backoff on rate limiting

            try {
                future.complete(amiAmiApiService.getItemDetails(AmiAmiItemDetailsRequest.builder()
                    .gCode(productQueryRequest.getProductCode())
                    .build()
                ).send().join());
            } catch (Exception e) {
                future.completeExceptionally(new QueryException("Error performing AmiAmi item detail query for product %s".formatted(
                    productQueryRequest.getProductCode()
                ), e));
            } finally {
                lastQueryTimeMillis = System.currentTimeMillis();
            }
        });

        return future;
    }

    private void enforceQueryInterval() {
        var config = configurationService.getProductQuery();
        long now = System.currentTimeMillis();
        long timeSinceLastQuery = now - lastQueryTimeMillis;
        if (timeSinceLastQuery < config.getApiQueryMinIntervalMillis()) {
            long sleepTime = config.getApiQueryMinIntervalMillis() - timeSinceLastQuery;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastQueryTimeMillis = now;
    }
}
