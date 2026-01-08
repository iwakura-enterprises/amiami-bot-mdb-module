package enterprises.iwakura.amitracker.service;

import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.repository.ProductRepository;
import enterprises.iwakura.amitracker.objects.query.ProductQueryRequest;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AmiAmiQueryService amiAmiQueryService;
    private final ProductProcessorService productProcessorService;
    private final ProductImageService productImageService;

    /**
     * Get a product by its code, or query AmiAmi if not found.
     *
     * @param productCode the product code
     *
     * @return the product entity
     */
    public Optional<ProductEntity> getOrQueryProduct(String productCode) {
        var optionalProduct = productRepository.findByCode(productCode);

        if (optionalProduct.isPresent()) {
            var product = optionalProduct.get();

            if (product.getLastImageUpdateAt() == null) {
                // Schedule image fetching if not done before asynchronously
                productImageService.fetchImageUrl(product.getImageUrl());
            }

            return optionalProduct;
        } else {
            log.info("Product with code {} not found in database, scheduling an query", productCode);
            var response = amiAmiQueryService.scheduleItemDetail(new ProductQueryRequest(productCode)).join();
            return productProcessorService.process(response);
        }
    }
}
