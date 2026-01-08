package enterprises.iwakura.amitracker.service;

import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.constant.ProductState;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.repository.ProductRepository;
import enterprises.iwakura.amitracker.util.ReleaseDateParser;
import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse;
import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse.Item;
import enterprises.iwakura.kirara.amiami.response.AmiAmiSearchResponse;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductProcessorService {

    private final DatabaseService databaseService;
    private final ProductHistoryService productHistoryService;
    private final ProductImageService productImageService;

    private final ProductRepository productRepository;

    public void process(List<AmiAmiSearchResponse> searchResponses) {
        log.info("Processing search responses: {}", searchResponses);
        // TODO:
        //  - Get previous results from DB
        //  - Compare with new results
        //    - New items -> if on first page, classify as new item
        //    - Status changes
        //    - If removed - Fetch details to confirm removal.
        //      - If no changes, somehow tell to not track again (e.g. outside of the search results?)
        //      - Update DB with new results, if any -> update
        //  - Always for all fetched items create ProductEntity and for existing product the price history entry.
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

        log.info("Processing item response: {}", itemResponse);
        var item = itemResponse.getItem();
        var optionalExistingProduct = productRepository.findByCode(item.getGCode());
        if (optionalExistingProduct.isPresent()) {
            var existingProduct = optionalExistingProduct.get();

            // Update existing product entity & check for changes
            // TODO

            return Optional.of(existingProduct);
        } else {
            // Create new product
            // No need to send any notifications here, as this product
            // should not have any wishlists yet.
            return Optional.of(createProductFromItem(item));
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
        var newSavedProduct = databaseService.runInThreadTransaction(session -> {
            var product = new ProductEntity();
            product.setCode(item.getGCode());
            product.setName(item.getName());
            product.setImageUrl(item.getMainImageUrl());
            product.setPriceJpy((long) item.getPriceJpy());
            product.setMakerName(item.getMakerName());
            product.setProductState(ProductState.parse(item));
            product.setReleaseDate(ReleaseDateParser.parse(item.getReleaseDate()));
            var savedProduct = productRepository.save(product);
            productHistoryService.initialize(savedProduct);
            return savedProduct;
        });
        // Caches the image URL asynchronously
        productImageService.fetchImageUrl(newSavedProduct.getImageUrl());
        return newSavedProduct;
    }
}
