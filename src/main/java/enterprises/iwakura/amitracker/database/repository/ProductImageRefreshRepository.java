package enterprises.iwakura.amitracker.database.repository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import enterprises.iwakura.amitracker.constant.ImageRefreshReason;
import enterprises.iwakura.amitracker.constant.QueueState;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductImageRefreshEntity;
import enterprises.iwakura.amitracker.service.ConfigurationService;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
public class ProductImageRefreshRepository extends AmiBaseRepository<ProductImageRefreshEntity, Long> {

    private final ConfigurationService configurationService;

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ProductImageRefreshRepository(DatabaseService databaseService, ConfigurationService configurationService) {
        super(databaseService);
        this.configurationService = configurationService;
    }

    @Override
    public Class<ProductImageRefreshEntity> getEntityClass() {
        return ProductImageRefreshEntity.class;
    }

    @Override
    protected boolean hasId(ProductImageRefreshEntity productImageRefreshEntity) {
        return productImageRefreshEntity.getId() != null;
    }

    /**
     * Finds all pending {@link ProductImageRefreshEntity}
     * @return List of {@link ProductImageRefreshEntity}
     */
    public List<ProductImageRefreshEntity> findAllPending() {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                  FROM ProductImageRefreshEntity p
                  WHERE p.state = 'QUEUED' AND p.refreshAfter IS NULL OR p.refreshAfter <= :now
                  """;

            return session.createQuery(hql, ProductImageRefreshEntity.class)
                .setParameter("now", OffsetDateTime.now())
                .getResultList();
        });
    }

    /**
     * Creates new image refreshment for the specified product entity if one does not exist already
     *
     * @param product Product
     * @param reason  Reason
     */
    public void createPending(ProductEntity product, ImageRefreshReason reason) {
        databaseService.runInThreadTransaction(session -> {
            var exists = session.createQuery(
                    "SELECT COUNT(p) FROM ProductImageRefreshEntity p WHERE p.product = :product",
                    Long.class
                )
                .setParameter("product", product)
                .getSingleResult() > 0;

            if (!exists) {
                log.info("Creating ProductImageRefreshEntity for product {}", product.getCode());
                var entity = new ProductImageRefreshEntity();
                entity.setProduct(product);
                entity.setState(QueueState.QUEUED);
                entity.setRefreshReason(reason);
                entity.setRefreshAfter(
                    OffsetDateTime.now().plus(
                        configurationService.getProductQuery().getNoImageRefreshBackOffBase(),
                        ChronoUnit.MILLIS
                    )
                );

                save(entity);
            }
        });
    }
}
