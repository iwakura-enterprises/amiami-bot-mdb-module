package enterprises.iwakura.amitracker.service;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductHistoryEntity;
import enterprises.iwakura.amitracker.database.repository.ProductHistoryRepository;
import enterprises.iwakura.kirara.amiami.response.AmiAmiItemResponse.Item;
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
     * @param product the ProductEntity to base the history on
     *
     * @return the initialized ProductHistoryEntity
     *
     * @throws IllegalStateException if the product does not have state or price initialized
     */
    public ProductHistoryEntity initialize(ProductEntity product) {
        if (product.getProductState() == null || product.getPriceJpy() == null) {
            throw new IllegalStateException("Product must have state and price initialized before creating history.");
        }

        var productHistory = new ProductHistoryEntity();
        productHistory.setPriceJpy(product.getPriceJpy());
        productHistory.setProductState(product.getProductState());
        productHistory.setProduct(product);
        var savedProductHistory = productHistoryRepository.save(productHistory);
        product.getHistory().add(savedProductHistory);
        return savedProductHistory;
    }
}
