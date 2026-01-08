package enterprises.iwakura.amitracker.database.repository;

import java.time.OffsetDateTime;
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
            var hql = """
                FROM ProductEntity p
                WHERE p.code = :productCode
                """;
            var query = session.createQuery(hql, ProductEntity.class);
            query.setParameter("productCode", productCode.toUpperCase());
            return query.uniqueResultOptional();
        });
    }

    /**
     * Updates the last image update date for a product.
     *
     * @param productCode the product code
     * @param date        the new last image update date
     */
    public void updateLastImageUpdate(String productCode, OffsetDateTime date) {
        databaseService.runInThreadTransaction(session -> {
            var hql = """
                UPDATE ProductEntity p
                SET p.lastImageUpdateAt = :date
                WHERE p.code = :productCode
                """;
            var query = session.createQuery(hql, null);
            query.setParameter("date", date);
            query.setParameter("productCode", productCode.toUpperCase());
            query.executeUpdate();
        });
    }

    /**
     * Checks if a product has a last image update date.
     *
     * @param productCode the product code
     *
     * @return true if the product has a last image update date, false otherwise
     */
    public boolean hasLastImageUpdate(String productCode) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                SELECT p.lastImageUpdateAt
                FROM ProductEntity p
                WHERE p.code = :productCode
                """;
            var query = session.createQuery(hql, OffsetDateTime.class);
            query.setParameter("productCode", productCode.toUpperCase());
            var result = query.uniqueResultOptional();
            return result.isPresent();
        });
    }
}
