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
            query.setParameter("thresholdTime", OffsetDateTime.now().minus(System.currentTimeMillis() - intervalMillis, ChronoUnit.MILLIS));
            return query.list();
        });
    }
}
