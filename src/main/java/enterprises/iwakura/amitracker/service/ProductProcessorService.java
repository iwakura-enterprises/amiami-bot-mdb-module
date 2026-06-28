package enterprises.iwakura.amitracker.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import enterprises.iwakura.amitracker.constant.ImageRefreshReason;
import enterprises.iwakura.amitracker.constant.ProductChangeType;
import enterprises.iwakura.amitracker.constant.ProductState;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.database.repository.ProductImageRefreshRepository;
import enterprises.iwakura.amitracker.database.repository.ProductListQueryRepository;
import enterprises.iwakura.amitracker.database.repository.ProductQueryResultEntryRepository;
import enterprises.iwakura.amitracker.database.repository.ProductRepository;
import enterprises.iwakura.amitracker.object.ProductChangeHolder;
import enterprises.iwakura.amitracker.service.scheduler.ProductQueryScheduler;
import enterprises.iwakura.amitracker.util.ReleaseDateParser;
import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse.Item;
import enterprises.iwakura.kirara.amiami.response.AmiAmiSearchResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiSearchResponse.ResultItem;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductProcessorService {

    private final DatabaseService databaseService;
    private final ProductHistoryService productHistoryService;
    private final ProductChangeAnnounceService productChangeAnnounceService;

    private final ProductRepository productRepository;
    private final ProductListQueryRepository productListQueryRepository;
    private final ProductQueryResultEntryRepository productQueryResultEntryRepository;
    private final ProductImageRefreshRepository productImageRefreshRepository;

    private final Gson gson;

    @Bean
    private final BeanAccessor<ProductQueryScheduler> productQuerySchedulerBeanAccessor = new BeanAccessor<>(
        ProductQueryScheduler.class);

    public void process(ProductListQueryEntity productListQueryEntity, List<AmiAmiSearchResponse> searchResponses) {
        databaseService.runInThreadTransaction(session -> {
            if (searchResponses.isEmpty()) {
                log.warn("Ignoring empty search response");
                return;
            }

            log.info("Processing {} search responses for product list query {}", searchResponses.size(), productListQueryEntity.getId());
            productListQueryEntity.setResponseJson(gson.toJson(searchResponses));

            var resultItemByCode = searchResponses.stream()
                .flatMap(response -> response.getItems().stream())
                // Map by gcode, just take first item if duplicate (rare, happens when two pages contain one item)
                .collect(Collectors.toMap(ResultItem::getGCode, Function.identity(), (k1, k2) -> k1));
            var productCodes = resultItemByCode.values().stream()
                .map(ResultItem::getGCode)
                .toList();

            var newProductCodesInListQuery = productListQueryRepository.findNewProductCodes(productListQueryEntity.getId(),
                productCodes);

            if (!newProductCodesInListQuery.isEmpty()) {
                log.info("Found new product codes for product list query {}: {}",
                    productListQueryEntity.getId(), newProductCodesInListQuery
                );

                var nonExistingProductCodes = productRepository.findNewProductCodes(newProductCodesInListQuery);
                var existingProductCodes = newProductCodesInListQuery.stream()
                    .filter(it -> !nonExistingProductCodes.contains(it)).toList();

                nonExistingProductCodes.forEach(newProductCode -> {
                    var resultItem = resultItemByCode.get(newProductCode);

                    if (resultItem != null) {
                        // Create new product & add it to the product list query entity
                        var productEntity = createProductFromResultItem(resultItem);
                        productQueryResultEntryRepository.createFor(productListQueryEntity, productEntity);

                        if (!productListQueryEntity.isSkipNextProductAddOrRemoveChangeAnnouncements()) {
                            productChangeAnnounceService.scheduleProductAddedInList(productListQueryEntity, productEntity);
                        } else {
                            //noinspection LoggingSimilarMessage
                            log.warn("Skipping new product announcement for product code {} in product list {}",
                                productEntity.getCode(), productListQueryEntity.getId()
                            );
                        }
                    } else {
                        log.error("This should not happen! Result item not found from newProductCode {} when adding products to a list {}",
                            newProductCode, productListQueryEntity.getId()
                        );
                    }
                });

                // Fetch existing product entities
                if (!existingProductCodes.isEmpty()) {
                    productRepository.findByCodes(existingProductCodes).forEach(productEntity -> {
                        var resultItem = resultItemByCode.get(productEntity.getCode());

                        // Add the product into the product list query entity
                        productQueryResultEntryRepository.createFor(productListQueryEntity, productEntity);

                        if (resultItem != null) {
                            if (!productListQueryEntity.isSkipNextProductAddOrRemoveChangeAnnouncements()) {
                                productChangeAnnounceService.scheduleProductAddedInList(productListQueryEntity, productEntity);
                            } else {
                                //noinspection LoggingSimilarMessage
                                log.warn("Skipping new product announcement for product code {} in product list {}",
                                    productEntity.getCode(), productListQueryEntity.getId()
                                );
                            }

                            // Update existing product and check for changes
                            productEntity.setResponseJson(gson.toJson(resultItem));
                            productRepository.save(productEntity);
                            checkChangesAndUpdateAndAnnounce(
                                productEntity,
                                resultItem.getMinimumPriceJpy(),
                                ProductState.parse(resultItem),
                                resultItem.getThumbnailUrl()
                            );
                        } else {
                            log.error(
                                "This should not happen! Result item not found from ProductEntity code {} when adding products to a list {}",
                                productEntity.getCode(), productListQueryEntity.getId()
                            );
                        }
                    });
                }
            }

            // Updates products already in the list and are not new
            // Must exist in the product table as it is linked with this ProductListQueryEntity
            var remainingProductCodes = productCodes.stream()
                .filter(item -> !newProductCodesInListQuery.contains(item))
                .toList();
            if (!remainingProductCodes.isEmpty()) {
                productRepository.findByCodes(remainingProductCodes).forEach(productEntity -> {
                    var resultItem = resultItemByCode.get(productEntity.getCode());

                    if (resultItem != null) {
                        // Update existing product entity & check for changes
                        productEntity.setResponseJson(gson.toJson(resultItem));
                        productRepository.save(productEntity);
                        checkChangesAndUpdateAndAnnounce(productEntity, resultItem.getMinimumPriceJpy(), ProductState.parse(resultItem), resultItem.getThumbnailUrl());
                    } else {
                        log.error("This should not happen! Result item not found from ProductEntity code {} when updating remaining products",
                            productEntity.getCode()
                        );
                    }
                });
            }

            if (productListQueryEntity.isSkipNextProductAddOrRemoveChangeAnnouncements()) {
                log.info("The next product list query process for ID {} will announce new/removed products", productListQueryEntity.getId());
                productListQueryEntity.setSkipNextProductAddOrRemoveChangeAnnouncements(false);
                productListQueryRepository.save(productListQueryEntity);
            }
        });
    }

    /**
     * Process an AmiAmi item response to extract or update a ProductEntity.
     *
     * @param itemResponse the AmiAmi item response
     *
     * @return an Optional containing the ProductEntity if processing was successful, otherwise empty
     */
    public Optional<ProductEntity> process(AmiAmiItemResponse itemResponse) {
        if (itemResponse == null
            || !itemResponse.isSuccessful()
            || itemResponse.getItem() == null
        ) {
            return Optional.empty();
        }

        log.info("Processing item response with code {}", itemResponse.getItem().getGCode());

        var item = itemResponse.getItem();
        var optionalExistingProduct = productRepository.findByCode(item.getGCode());
        if (optionalExistingProduct.isPresent()) {
            var existingProduct = optionalExistingProduct.get();
            existingProduct.setResponseJson(gson.toJson(item));

            // Update existing product entity & check for changes
            checkChangesAndUpdateAndAnnounce(existingProduct, item.getPriceJpy(), ProductState.parse(item), item.getMainImageUrl());

            existingProduct.setUpdatedAt(OffsetDateTime.now());
            productRepository.save(existingProduct);
            return Optional.of(existingProduct);
        } else {
            // Create new product
            // No need to send any notifications here, as this product
            // should not have any wishlists or product list queries yet.
            return Optional.of(createProductFromItem(item));
        }
    }

    /**
     * Checks if specified attributes have changed on the product. If so, updates it and schedules an announcement for
     * that
     *
     * @param productEntity   Product entity
     * @param newPriceJpy     New price in JPY
     * @param newProductState New product state
     */
    public void checkChangesAndUpdateAndAnnounce(
        ProductEntity productEntity,
        long newPriceJpy,
        ProductState newProductState,
        String newImageUrl
    ) {
        final var oldPriceJpy = productEntity.getPriceJpy();
        final var oldProductState = productEntity.getProductState();
        final var oldImageUrl = productEntity.getImageUrl();

        boolean priceChanged = !Objects.equals(oldPriceJpy, newPriceJpy) && newPriceJpy > 0; // ignore new price being 0
        boolean priceHasDiscount = priceChanged && newPriceJpy < oldPriceJpy;
        boolean productStateChanged = !Objects.equals(oldProductState, newProductState);
        boolean imageUrlChanged = !Objects.equals(oldImageUrl, newImageUrl) && !newImageUrl.equalsIgnoreCase(AmiAmiApiService.NO_IMAGE_URL); // Guard against setting the image to no image
        List<ProductChangeType> productChangeTypes = new ArrayList<>();

        if (productStateChanged) {
            productChangeTypes.add(ProductChangeType.PRODUCT_STATE_CHANGED);
        }
        if (priceHasDiscount) {
            productChangeTypes.add(ProductChangeType.PRICE_DISCOUNT);
        }
        if (imageUrlChanged) {
            productChangeTypes.add(ProductChangeType.IMAGE_URL_CHANGE);
            productEntity.setImageUrl(newImageUrl);
        }

        // Save even if price is not of discount
        if (productStateChanged || priceChanged) {
            // Add history
            productHistoryService.addNewHistory(productEntity, newPriceJpy, newProductState, productEntity.getResponseJson());

            // Update values
            productEntity.setPriceJpy(newPriceJpy);
            productEntity.setProductState(newProductState);
            productRepository.save(productEntity);
        }

        if (!productChangeTypes.isEmpty()) {
            log.info("Product {} has some changed tracked attribute (price {} -> {}, state {} -> {}, image url {} -> {}), resolved as {}",
                productEntity.getCode(),
                oldPriceJpy, newPriceJpy,
                oldProductState, newProductState,
                oldImageUrl, newImageUrl,
                productChangeTypes
            );

            productChangeAnnounceService.schedule(
                productEntity,
                productChangeTypes,
                ProductChangeHolder.builder()
                    .oldPriceJpy(oldPriceJpy).newPriceJpy(newPriceJpy)
                    .oldProductState(oldProductState).newProductState(newProductState)
                    .build()
            );
        }
    }

    /**
     * Creates a new ProductEntity from the given AmiAmi item.
     *
     * @param item the AmiAmi item
     *
     * @return the created ProductEntity
     */
    private ProductEntity createProductFromItem(Item item) {
        return databaseService.runInThreadTransaction(session -> {
            var json = gson.toJson(item);

            var product = new ProductEntity();
            product.setCode(item.getGCode());
            product.setName(item.getName());
            product.setImageUrl(item.getMainImageUrl());
            product.setPriceJpy((long) item.getPriceJpy());
            product.setMakerName(item.getMakerName());
            product.setProductState(ProductState.parse(item));
            product.setReleaseDate(ReleaseDateParser.parse(item.getReleaseDate()));
            product.setResponseJson(json);
            var savedProduct = productRepository.save(product);
            productHistoryService.initialize(savedProduct, json);

            if (product.getImageUrl().equalsIgnoreCase(AmiAmiApiService.NO_IMAGE_URL)) {
                //productImageRefreshRepository.createPending(product, ImageRefreshReason.NO_IMAGE);
            }

            return savedProduct;
        });
    }

    /**
     * Creates a new ProductEntity from the given AmiAmi search result item.
     *
     * @param resultItem The AmiAmi search result item
     *
     * @return The created ProductEntity
     */
    private ProductEntity createProductFromResultItem(ResultItem resultItem) {
        return databaseService.runInThreadTransaction(session -> {
            var json = gson.toJson(resultItem);

            var product = new ProductEntity();
            product.setCode(resultItem.getGCode());
            product.setName(resultItem.getGName());
            product.setImageUrl(resultItem.getThumbnailUrl());
            product.setPriceJpy(resultItem.getMinimumPriceJpy());
            product.setMakerName(resultItem.getMakerName());
            product.setProductState(ProductState.parse(resultItem));
            product.setReleaseDate(ReleaseDateParser.parseNormal(resultItem.getReleaseDate()));
            product.setResponseJson(json);
            var savedProduct = productRepository.save(product);
            productHistoryService.initialize(savedProduct, json);

            if (product.getImageUrl().equalsIgnoreCase(AmiAmiApiService.NO_IMAGE_URL)) {
                //productImageRefreshRepository.createPending(product, ImageRefreshReason.NO_IMAGE);
            }

            return savedProduct;
        });
    }
}
