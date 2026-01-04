package enterprises.iwakura.amitracker.service.scheduler;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.WishlistEntryEntity;
import enterprises.iwakura.amitracker.database.repository.ProductListQueryRepository;
import enterprises.iwakura.amitracker.database.repository.WishlistEntryRepository;
import enterprises.iwakura.amitracker.exception.MissingEntityException;
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

    private final AmiAmiQueryService amiAmiQueryService;
    private final ProductProcessorService productProcessorService;

    private final List<Long> scheduledProductListQueryIds = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> scheduledWishlistEntryIds = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void initialize() {
        log.info("Initializing ProductQueryScheduler...");
        this.schedule("ProductQueryFindPending", 0, 10, TimeUnit.SECONDS, this::processPendingQueries);
        this.schedule("WishlistEntryFindPending", 5, 10, TimeUnit.SECONDS, this::processWishlistProducts);
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
                pendingWishlistEntries.forEach(entry -> concurrencyService.scheduleQuery(() -> runProductQuery(entry)));
            }
        });
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
            var firstPage = amiAmiQueryService.scheduleSearch(new ProductListQueryRequest(productListQueryEntity.getId(), 1)).join();

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
                    var pageResult = amiAmiQueryService.scheduleSearch(new ProductListQueryRequest(productListQueryEntity.getId(), page)).join();
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

            productProcessorService.process(pageResponses);
        } catch (MissingEntityException exception) {
            log.warn("After scheduling and before processing, the ProductListQueryEntity with ID {} was not found. It may have been deleted.",
                productListQueryEntity.getId()
            );
        } catch (Exception exception) {
            log.error("Error processing ProductListQueryEntity with ID {}", productListQueryEntity.getId(), exception);
        } finally {
            scheduledProductListQueryIds.remove(productListQueryEntity.getId());
        }
    }

    /**
     * Schedules a WishlistEntryEntity for processing if it is not already scheduled.
     *
     * @param wishlistEntry the WishlistEntryEntity to schedule
     */
    public void runProductQuery(WishlistEntryEntity wishlistEntry) {
        synchronized (scheduledWishlistEntryIds) {
            if (scheduledWishlistEntryIds.contains(wishlistEntry.getId())) {
                log.debug("WishlistEntryEntity with ID {} is already scheduled. Skipping.",
                    wishlistEntry.getId()
                );
                return;
            }

            scheduledWishlistEntryIds.add(wishlistEntry.getId());
            log.debug("Scheduled WishlistEntryEntity with ID {} for processing.", wishlistEntry.getId());
        }

        try {
            var productCode = wishlistEntry.getProduct().getCode();
            // Schedules item detail query; respects rate limiting, interval settings and checks whenever it was recently queried.
            var itemResponse = amiAmiQueryService.scheduleItemDetail(new ProductQueryRequest(productCode)).join();

            if (!itemResponse.isSuccessful()) {
                log.error("Failed to fetch item details for WishlistEntryEntity with ID {} and product code {}. Errors: {}",
                    wishlistEntry.getId(),
                    productCode,
                    itemResponse.getResponseValue()
                );
                return;
            }

            productProcessorService.process(itemResponse);
        } catch (Exception exception) {
            log.error("Error processing WishlistEntryEntity with ID {}", wishlistEntry.getId(), exception);
        } finally {
            scheduledWishlistEntryIds.remove(wishlistEntry.getId());
        }
    }
}
