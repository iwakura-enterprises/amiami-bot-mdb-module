package enterprises.iwakura.amitracker.database.repository;

import java.util.List;

import enterprises.iwakura.amitracker.constant.QueueState;
import enterprises.iwakura.amitracker.database.entity.ProductChangeAnnouncementEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductChangeAnnouncementRepository extends AmiBaseRepository<ProductChangeAnnouncementEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ProductChangeAnnouncementRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<ProductChangeAnnouncementEntity> getEntityClass() {
        return ProductChangeAnnouncementEntity.class;
    }

    @Override
    protected boolean hasId(ProductChangeAnnouncementEntity productChangeAnnouncementEntity) {
        return productChangeAnnouncementEntity.getId() != null;
    }

    /**
     * Finds queued ProductChangeAnnouncementEntities
     *
     * @return ProductChangeAnnouncementEntities
     */
    public List<ProductChangeAnnouncementEntity> findAllQueued() {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      FROM ProductChangeAnnouncementEntity
                      WHERE announcementState = :state AND (wishlist IS NOT NULL OR channelProductListQuery IS NOT NULL)
                      """;
            return session.createQuery(hql, ProductChangeAnnouncementEntity.class)
                .setParameter("state", QueueState.QUEUED)
                .getResultList();
        });
    }
}
