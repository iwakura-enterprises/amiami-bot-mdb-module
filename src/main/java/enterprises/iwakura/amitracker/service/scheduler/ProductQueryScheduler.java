package enterprises.iwakura.amitracker.service.scheduler;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import enterprises.iwakura.amitracker.constant.QueueState;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.WishlistEntryEntity;
import enterprises.iwakura.amitracker.database.repository.ProductImageRefreshRepository;
import enterprises.iwakura.amitracker.database.repository.ProductListQueryRepository;
import enterprises.iwakura.amitracker.database.repository.ProductRepository;
import enterprises.iwakura.amitracker.database.repository.WishlistEntryRepository;
import enterprises.iwakura.amitracker.exception.MissingEntityException;
import enterprises.iwakura.amitracker.exception.QueryFailedException;
import enterprises.iwakura.amitracker.objects.query.ProductListQueryRequest;
import enterprises.iwakura.amitracker.objects.query.ProductQueryRequest;
import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import enterprises.iwakura.amitracker.service.AmiAmiQueryService;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.ConfigurationService;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.amitracker.service.ProductProcessorService;
import enterprises.iwakura.kirara.amiami.response.AmiAmiSearchResponse;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductQueryScheduler extends BaseScheduler {

    private final ConfigurationService configurationService;
    private final ConcurrencyService concurrencyService;
    private final DatabaseService databaseService;
    private final ProductListQueryRepository productListQueryRepository;
    private final WishlistEntryRepository wishlistEntryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRefreshRepository productImageRefreshRepository;

    private final AmiAmiQueryService amiAmiQueryService;
    private final ProductProcessorService productProcessorService;

    private final List<Long> scheduledProductListQueryIds = Collections.synchronizedList(new ArrayList<>());
    private final List<String> scheduledProductCodeQueries = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void initialize() {
        log.info("Initializing ProductQueryScheduler...");
        this.schedule("ProductQueryFindPending", 0, 10, TimeUnit.SECONDS, this::processPendingQueries);
        this.schedule("WishlistEntryFindPending", 5, 10, TimeUnit.SECONDS, this::processWishlistProducts);
        this.schedule("ProductImageRefreshFindPending", 10, 60, TimeUnit.SECONDS, this::processProductImageRefresh);
    }

    /**
     * Finds and schedules all product queries that should be processed based on the configured interval.
     */
    private void processPendingQueries() {
        var productQueryConfig = configurationService.getProductQuery();
        databaseService.runInThreadTransaction(session -> {
            var pendingQueries = productListQueryRepository.findAllPending(productQueryConfig.getSearchQueryIntervalMillis());

            if (!pendingQueries.isEmpty()) {
                var ids = pendingQueries.stream().map(q -> q.getId().toString()).reduce((a, b) -> a + ", " + b).orElse("");
                log.debug("Found {} pending product queries to process: {}", pendingQueries.size(), ids);
                pendingQueries.forEach(query -> {
                    query.setLastQueryAt(OffsetDateTime.now());
                    productListQueryRepository.save(query);
                });
                pendingQueries.forEach(query -> concurrencyService.scheduleQuery(() -> runProductQuery(query)));
            }
        });
    }

    /**
     * Finds and schedules all wishlist products that should be processed based on the configured interval.
     */
    private void processWishlistProducts() {
        var productQueryConfig = configurationService.getProductQuery();
        databaseService.runInThreadTransaction(session -> {
            var pendingWishlistEntries = wishlistEntryRepository.findAllPending(productQueryConfig.getItemDetailQueryIntervalMillis());

            if (!pendingWishlistEntries.isEmpty()) {
                var ids = pendingWishlistEntries.stream().map(e -> e.getId().toString()).reduce((a, b) -> a + ", " + b).orElse("");
                log.debug("Found {} pending wishlist entries to process: {}", pendingWishlistEntries.size(), ids);
                pendingWishlistEntries.forEach(entry -> concurrencyService.scheduleQuery(() -> runProductQuery(entry.getProduct().getCode())));
            }
        });
    }

    /**
     * Finds and schedules all product image refreshments
     */
    private void processProductImageRefresh() {
        var productQueryConfig = configurationService.getProductQuery();

        // Get image refreshments to process
        var pendingImageRefreshments = databaseService.runInThreadTransaction(outerSession -> {
            var results = productImageRefreshRepository.findAllPending();
            results.forEach(it -> it.setState(QueueState.PROCESSING));
            results.forEach(productImageRefreshRepository::save); // Save
            return results;
        });

        if (pendingImageRefreshments.isEmpty()) {
            return;
        }

        var ids = pendingImageRefreshments.stream().map(e -> e.getId().toString()).reduce((a, b) -> a + ", " + b).orElse("");
        log.debug("Found {} pending image refreshments to process: {}", pendingImageRefreshments.size(), ids);
        pendingImageRefreshments.forEach(entry -> concurrencyService.scheduleQuery(() -> {
            // Run the query, should update ProductEntity with new imageUrl, if found
            runProductQuery(entry.getProduct().getCode());

            // Run transaction after the product query so we have new stuff
            databaseService.runInThreadTransaction(session -> {
                var optionalProductRefreshEntity = productImageRefreshRepository.findById(entry.getId());

                if (optionalProductRefreshEntity.isPresent()) {
                    var productRefreshEntity = optionalProductRefreshEntity.get();

                    var optionalProduct = productRepository.findByCode(entry.getProduct().getCode());
                    if (optionalProduct.isPresent()) {
                        var product = optionalProduct.get();

                        switch (productRefreshEntity.getRefreshReason()) {
                            case NO_IMAGE -> {
                                if (product.getImageUrl().equalsIgnoreCase(AmiAmiApiService.NO_IMAGE_URL)) {
                                    // Still no image
                                    int retryNumber = Optional.ofNullable(productRefreshEntity.getRetryNumber()).orElse(0);

                                    if (retryNumber >= productQueryConfig.getNoImageMaxRetryCount()) {
                                        log.warn("Image for product {} not found even after {} retries! Too bad. Removing product image refresh entity {}",
                                            product.getCode(), retryNumber, productRefreshEntity.getId()
                                        );
                                        productImageRefreshRepository.delete(productRefreshEntity);
                                    } else {
                                        retryNumber = retryNumber + 1;
                                        var nextSchedule = OffsetDateTime.now().plus(
                                            (long) Math.pow(
                                                productQueryConfig.getNoImageRefreshBackOffBase(),
                                                retryNumber
                                            ),
                                            ChronoUnit.MILLIS
                                        );
                                        log.info("Image for product {} not found, retry no. {}/{}, next try @ {} in product image refresh enttiy {}",
                                            product.getCode(), retryNumber, productQueryConfig.getNoImageMaxRetryCount(), nextSchedule, productRefreshEntity.getId()
                                        );
                                        productRefreshEntity.setRefreshAfter(nextSchedule);
                                        productRefreshEntity.setState(QueueState.QUEUED);
                                        productRefreshEntity.setRetryNumber(retryNumber);
                                        productImageRefreshRepository.save(productRefreshEntity);
                                    }
                                } else {
                                    // Found image! It should be sent to corresponding channels
                                    log.info("Image for product {} found, removing product image refresh entity {}: {}",
                                        product.getCode(), productRefreshEntity.getId(), product.getImageUrl()
                                    );
                                    productImageRefreshRepository.delete(productRefreshEntity);
                                }
                            }
                        }
                    } else {
                        log.error("Product not found by code in inner query scheduled runnable! Removing product image refresh entity {}",
                            productRefreshEntity.getId()
                        );
                        productImageRefreshRepository.delete(productRefreshEntity);
                    }
                } else {
                    log.error("ProductImageRefreshEntity {} not found in inner query scheduled runnable!", entry.getId());
                }
            });
        }));
    }

    /**
     * Schedules a ProductListQueryEntity for processing if it is not already scheduled.
     *
     * @param productListQueryEntity the ProductListQueryEntity to schedule
     */
    public void runProductQuery(ProductListQueryEntity productListQueryEntity) {
        synchronized (scheduledProductListQueryIds) {
            if (scheduledProductListQueryIds.contains(productListQueryEntity.getId())) {
                log.debug("ProductListQueryEntity with ID {} is already scheduled. Skipping.",
                    productListQueryEntity.getId()
                );
                return;
            }
            scheduledProductListQueryIds.add(productListQueryEntity.getId());
            log.debug("Scheduled ProductListQueryEntity with ID {} for processing.", productListQueryEntity.getId());
        }

        try {
            List<AmiAmiSearchResponse> pageResponses = new ArrayList<>();
            var firstPage = amiAmiQueryService.scheduleSearch(new ProductListQueryRequest(
                productListQueryEntity.getId(), productListQueryEntity.toAmiAmiSearchRequest(1))
            ).join();

            if (!firstPage.isSuccessful()) {
                log.error("Failed to fetch first page for ProductListQueryEntity with ID {}. Errors: {}",
                    productListQueryEntity.getId(),
                    firstPage.getResponseValue()
                );
                return;
            }

            pageResponses.add(firstPage);
            log.debug("Processing ProductListQueryEntity with ID {}: found {} products on first page.",
                productListQueryEntity.getId(),
                firstPage.getSearchResults().getTotalResults()
            );
            var totalPagesToFetch = Math.min(
                productListQueryEntity.getMaxPagination(),
                (int) Math.ceil((double) firstPage.getSearchResults().getTotalResults() / AmiAmiApiService.MAX_ITEMS_PER_QUERY)
            );
            if (totalPagesToFetch > 1) {
                for (int page = 2; page <= totalPagesToFetch; page++) {
                    var pageResult = amiAmiQueryService.scheduleSearch(new ProductListQueryRequest(
                        productListQueryEntity.getId(), productListQueryEntity.toAmiAmiSearchRequest(page))
                    ).join();

                    log.debug("Processing ProductListQueryEntity with ID {}: found {} products on page {}.",
                        productListQueryEntity.getId(),
                        pageResult.getItems().size(),
                        page
                    );
                    if (pageResult.isSuccessful()) {
                        pageResponses.add(pageResult);
                    } else {
                        log.error("Failed to fetch page {} for ProductListQueryEntity with ID {}, there may be missing results, errors: {}",
                            page,
                            productListQueryEntity.getId(),
                            pageResult.getResponseValue()
                        );
                    }
                }
            }

            databaseService.runInThreadTransaction(session -> {
                var refreshedEntity = productListQueryRepository.findById(productListQueryEntity.getId()).orElseThrow(() ->
                    new MissingEntityException("ProductListQueryEntity[%d] not found".formatted(
                        productListQueryEntity.getId()
                    ))
                );

                // Update to actual last query time
                refreshedEntity.setLastQueryAt(OffsetDateTime.now());
                refreshedEntity.setTotalItemsCount(firstPage.getSearchResults().getTotalResults());
                productListQueryRepository.save(refreshedEntity);
            });

            productProcessorService.process(productListQueryEntity, pageResponses);
        } catch (MissingEntityException exception) {
            log.warn("After scheduling and before processing, the ProductListQueryEntity with ID {} was not found. It may have been deleted.",
                productListQueryEntity.getId()
            );
        } catch (QueryFailedException exception) {
            log.error("Query failed while processing ProductListQueryEntity with ID {}: {}",
                productListQueryEntity.getId(), exception.getMessage(), exception
            );
        } catch (Exception exception) {
            log.error("Error processing ProductListQueryEntity with ID {}", productListQueryEntity.getId(), exception);
        } finally {
            scheduledProductListQueryIds.remove(productListQueryEntity.getId());
        }
    }

    /**
     * Schedules an arbitrary product code query.
     *
     * @param productCode the product code
     */
    public void runProductQuery(String productCode) {
        synchronized (scheduledProductCodeQueries) {
            if (scheduledProductCodeQueries.contains(productCode)) {
                log.debug("Product code {} is already scheduled. Skipping.",
                    productCode
                );
                return;
            }

            scheduledProductCodeQueries.add(productCode);
            log.debug("Scheduled product code {} for processing.", productCode);
        }

        try {
            // Schedules item detail query; respects rate limiting, interval settings and checks whenever it was recently queried.
            var itemResponse = amiAmiQueryService.scheduleItemDetail(new ProductQueryRequest(productCode)).join();

            if (!itemResponse.isSuccessful()) {
                log.error("Failed to fetch item details for product code {}. Errors: {}",
                    productCode,
                    itemResponse.getResponseValue()
                );
                return;
            }

            productProcessorService.process(itemResponse);
        } catch (QueryFailedException exception) {
            log.error("Query failed while processing product code {}: {}",
                productCode, exception.getMessage(), exception
            );
        } catch (Exception exception) {
            log.error("Error processing product code {}", productCode, exception);
        } finally {
            scheduledProductCodeQueries.remove(productCode);
        }
    }
}
