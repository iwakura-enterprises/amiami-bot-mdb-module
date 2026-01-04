package enterprises.iwakura.amitracker.database.repository;

import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductRepository extends AmiBaseRepository<ProductEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ProductRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<ProductEntity> getEntityClass() {
        return ProductEntity.class;
    }

    @Override
    protected boolean hasId(ProductEntity productEntity) {
        return productEntity.getId() != null;
    }

    /**
     * Finds a product by its code.
     *
     * @param productCode the product code
     *
     * @return an optional containing the product entity if found, or empty if not found
     */
    public Optional<ProductEntity> findByCode(String productCode) {
        return databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                    FROM ProductEntity p
                    WHERE p.code = :productCode
                    """;
            var query = session.createQuery(hql, ProductEntity.class);
            query.setParameter("productCode", productCode.toUpperCase());
            return query.uniqueResultOptional();
        });
    }
}
