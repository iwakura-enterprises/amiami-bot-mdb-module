package enterprises.iwakura.amitracker.service;

import enterprises.iwakura.amitracker.constant.ProductState;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductHistoryEntity;
import enterprises.iwakura.amitracker.database.repository.ProductHistoryRepository;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductHistoryService {

    private final ProductHistoryRepository productHistoryRepository;

    /**
     * Initializes a ProductHistoryEntity based on the given ProductEntity.
     *
     * @param productEntity the ProductEntity to base the history on
     *
     * @return the initialized ProductHistoryEntity
     *
     * @throws IllegalStateException if the product does not have state or price initialized
     */
    public ProductHistoryEntity initialize(ProductEntity productEntity) {
        if (productEntity.getProductState() == null || productEntity.getPriceJpy() == null) {
            throw new IllegalStateException("Product must have state and price initialized before creating history.");
        }
        return productHistoryRepository.addNewHistory(
            productEntity, productEntity.getPriceJpy(), productEntity.getProductState()
        );
    }

    /**
     * Creates a new product history for a specified product with specified values
     *
     * @param productEntity   Product entity
     * @param newPriceJpy     New price in JPY
     * @param newProductState New product state
     *
     * @return Created product history entity
     */
    public ProductHistoryEntity addNewHistory(
        ProductEntity productEntity,
        long newPriceJpy,
        ProductState newProductState
    ) {
        return productHistoryRepository.addNewHistory(productEntity, newPriceJpy, newProductState);
    }
}
