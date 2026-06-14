package enterprises.iwakura.amitracker.database.repository;

import static enterprises.iwakura.amitracker.constant.Constants.DEFAULT_PRODUCT_LIST_QUERY_PAGINATION;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.object.ProductSearchParameters;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    /**
     * Gets or creates new {@link ProductListQueryEntity} based on the specified {@link ProductSearchParameters}
     *
     * @param productSearchParameters ProductSearchParameters
     *
     * @return ProductListQueryEntity
     */
    public ProductListQueryEntity getOrCreate(ProductSearchParameters productSearchParameters) {
        if (productSearchParameters.isEmpty()) {
            throw new IllegalArgumentException("Cannot create ProductListQueryEntity with empty ProductSearchParameters");
        }

        return databaseService.runInThreadTransaction(session -> {
            var sql = """
            SELECT id FROM product_query
            WHERE (searchKeywords = :searchKeywords OR (searchKeywords IS NULL AND CAST(:searchKeywords AS VARCHAR) IS NULL))
              AND (filterAnyAvailability = :filterAnyAvailability OR (filterAnyAvailability IS NULL AND CAST(:filterAnyAvailability AS BOOLEAN) IS NULL))
              AND (filterPreOrder = :filterPreOrder OR (filterPreOrder IS NULL AND CAST(:filterPreOrder AS BOOLEAN) IS NULL))
              AND (filterBackOrder = :filterBackOrder OR (filterBackOrder IS NULL AND CAST(:filterBackOrder AS BOOLEAN) IS NULL))
              AND (filterNewItems = :filterNewItems OR (filterNewItems IS NULL AND CAST(:filterNewItems AS BOOLEAN) IS NULL))
              AND (filterPreOwnedItems = :filterPreOwnedItems OR (filterPreOwnedItems IS NULL AND CAST(:filterPreOwnedItems AS BOOLEAN) IS NULL))
              AND (filterAmiAmiBonus = :filterAmiAmiBonus OR (filterAmiAmiBonus IS NULL AND CAST(:filterAmiAmiBonus AS BOOLEAN) IS NULL))
              AND (filterOnSaleItems = :filterOnSaleItems OR (filterOnSaleItems IS NULL AND CAST(:filterOnSaleItems AS BOOLEAN) IS NULL))
              AND (category1Id = :category1Id OR (category1Id IS NULL AND CAST(:category1Id AS INT) IS NULL))
              AND (category2Id = :category2Id OR (category2Id IS NULL AND CAST(:category2Id AS INT) IS NULL))
              AND (category3Id = :category3Id OR (category3Id IS NULL AND CAST(:category3Id AS INT) IS NULL))
              AND (category4Id = :category4Id OR (category4Id IS NULL AND CAST(:category4Id AS INT) IS NULL))
              AND (categoryTagId = :categoryTagId OR (categoryTagId IS NULL AND CAST(:categoryTagId AS INT) IS NULL))
              AND (characterNameId = :characterNameId OR (characterNameId IS NULL AND CAST(:characterNameId AS INT) IS NULL))
              AND (makerId = :makerId OR (makerId IS NULL AND CAST(:makerId AS INT) IS NULL))
              AND (originalTitleId = :originalTitleId OR (originalTitleId IS NULL AND CAST(:originalTitleId AS INT) IS NULL))
              AND (seriesTitleId = :seriesTitleId OR (seriesTitleId IS NULL AND CAST(:seriesTitleId AS INT) IS NULL))
            LIMIT 1
            """;

            var resultId = session.createNativeQuery(sql, Long.class)
                .setParameter("searchKeywords", productSearchParameters.getSearchKeywords())
                .setParameter("filterAnyAvailability", productSearchParameters.getFilterAnyAvailability())
                .setParameter("filterPreOrder", productSearchParameters.getFilterPreOrder())
                .setParameter("filterBackOrder", productSearchParameters.getFilterBackOrder())
                .setParameter("filterNewItems", productSearchParameters.getFilterNewItems())
                .setParameter("filterPreOwnedItems", productSearchParameters.getFilterPreOwnedItems())
                .setParameter("filterAmiAmiBonus", productSearchParameters.getFilterAmiAmiBonus())
                .setParameter("filterOnSaleItems", productSearchParameters.getFilterOnSaleItems())
                .setParameter("category1Id", productSearchParameters.getCategory1Id())
                .setParameter("category2Id", productSearchParameters.getCategory2Id())
                .setParameter("category3Id", productSearchParameters.getCategory3Id())
                .setParameter("category4Id", productSearchParameters.getCategory4Id())
                .setParameter("categoryTagId", productSearchParameters.getCategoryTagId())
                .setParameter("characterNameId", productSearchParameters.getCharacterNameId())
                .setParameter("makerId", productSearchParameters.getMakerId())
                .setParameter("originalTitleId", productSearchParameters.getOriginalTitleId())
                .setParameter("seriesTitleId", productSearchParameters.getSeriesTitleId())
                .uniqueResultOptional();

            if (resultId.isPresent()) {
                return session.get(ProductListQueryEntity.class, resultId.get());
            } else {
                log.info("Creating new ProductListQueryEntity with search parameters {}", productSearchParameters);
                var newProductListQuery = new ProductListQueryEntity();
                newProductListQuery.setSkipNextProductAddOrRemoveChangeAnnouncements(true);
                newProductListQuery.setProductSearchParameters(productSearchParameters);
                newProductListQuery.setGlobalTemplate(false);
                return save(newProductListQuery);
            }
        });
    }
}
