package enterprises.iwakura.amitracker.database.repository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductListQueryRepository extends AmiBaseRepository<ProductListQueryEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ProductListQueryRepository(
        DatabaseService databaseService
    ) {
        super(databaseService);
    }

    @Override
    public Class<ProductListQueryEntity> getEntityClass() {
        return ProductListQueryEntity.class;
    }

    @Override
    protected boolean hasId(ProductListQueryEntity productListQueryEntity) {
        return productListQueryEntity.getId() != null;
    }

    /**
     * Finds all pending product queries that have not been processed within the specified interval and are not marked
     * as dirty.
     *
     * @param intervalMillis the interval in milliseconds
     *
     * @return a list of pending product queries
     */
    public List<ProductListQueryEntity> findAllPending(long intervalMillis) {
        return databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                    FROM ProductListQueryEntity plq
                    WHERE plq.lastQueryAt IS NULL OR plq.lastQueryAt <= :thresholdTime
                    """;
            var query = session.createQuery(hql, ProductListQueryEntity.class);
            query.setParameter("thresholdTime",
                OffsetDateTime.now().minus(intervalMillis, ChronoUnit.MILLIS));
            return query.list();
        });
    }

    /**
     * Finds new product codes that are not present in the product list query entity
     *
     * @param productListQueryEntityId Product list query entity ID
     * @param productCodes             Product codes to check against
     *
     * @return List of new product codes
     */
    public List<String> findNewProductCodes(Long productListQueryEntityId, List<String> productCodes) {
        if (productCodes == null || productCodes.isEmpty()) {
            return List.of();
        }
        return databaseService.runInThreadTransaction(session -> {
            var sql = """
                SELECT unnest(CAST(:productCodes AS text[]))
                EXCEPT
                SELECT p.code
                FROM product_list_query_result_entry e
                JOIN product p ON p.id = e.product_id
                WHERE e.productListQuery_id = :queryId
                """;
            return session.createNativeQuery(sql, String.class)
                .setParameter("productCodes", productCodes.toArray(String[]::new))
                .setParameter("queryId", productListQueryEntityId)
                .list();
        });
    }

    /**
     * Finds product codes that are present in the stored results for the given query entity
     * but are not present in the provided list of product codes.
     *
     * @param productListQueryEntityId Product list query entity ID
     * @param productCodes             Current product codes to check against
     *
     * @return List
     * of removed product codes
     */
    public List<String> findRemovedProductCodes(Long productListQueryEntityId, List<String> productCodes) {
        if (productCodes == null || productCodes.isEmpty()) {
            // If no current codes provided, all stored codes are "removed"
            return databaseService.runInThreadTransaction(session -> {
                var sql = """
                    SELECT p.code
                    FROM product_list_query_result_entry e
                    JOIN product p ON p.id = e.product_id
                    WHERE e.productListQuery_id = :queryId
                    """;
                return session.createNativeQuery(sql, String.class)
                    .setParameter("queryId", productListQueryEntityId)
                    .list();
            });
        }
        return databaseService.runInThreadTransaction(session -> {
            var sql = """
                SELECT p.code
                FROM product_list_query_result_entry e
                JOIN product p ON p.id = e.product_id
                WHERE e.productListQuery_id = :queryId
                EXCEPT
                SELECT unnest(CAST(:productCodes AS text[]))
                """;
            return session.createNativeQuery(sql, String.class)
                .setParameter("queryId", productListQueryEntityId)
                .setParameter("productCodes", productCodes.toArray(String[]::new))
                .list();
        });
    }

    /**
     * Removes product list query result entries for the given query entity that match the provided product codes.
     *
     * @param id                  Product list query entity ID
     * @param removedProductCodes Product codes to remove from the query result entries
     */
    public void removeResultEntriesByProductCodes(Long id, List<String> removedProductCodes) {
        if (removedProductCodes == null || removedProductCodes.isEmpty()) {
            return;
        }
        databaseService.runInThreadTransaction(session -> {
            var sql = """
                DELETE FROM product_list_query_result_entry e
                USING product p
                WHERE e.product_id = p.id
                    AND e.productListQuery_id = :queryId
                    AND p.code IN (:productCodes)
                """;
            session.createNativeQuery(sql)
                .setParameter("queryId", id)
                .setParameter("productCodes", removedProductCodes)
                .executeUpdate();
            return null;
        });
    }
}
