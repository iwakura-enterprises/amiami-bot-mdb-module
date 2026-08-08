package enterprises.iwakura.amitracker.database.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param maxResults the maximum number of announcements to return, oldest first
     *
     * @return ProductChangeAnnouncementEntities
     */
    public List<ProductChangeAnnouncementEntity> findAllQueued(int maxResults) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      FROM ProductChangeAnnouncementEntity
                      WHERE announcementState = :state AND (wishlist IS NOT NULL OR channelProductListQuery IS NOT NULL)
                      ORDER BY createdAt ASC
                      """;
            return session.createQuery(hql, ProductChangeAnnouncementEntity.class)
                .setParameter("state", QueueState.QUEUED)
                .setMaxResults(maxResults)
                .getResultList();
        });
    }

    /**
     * Finds the last previous ProductChangeAnnouncementEntity for the same product in the same channel
     * (via channelProductListQuery.channel), ignoring wishlist announcements.
     *
     * @param entity                          the entity to find the last previous announcement for
     * @param otherProductChangeAnnouncements Additionally ignores specified product change announcements
     *
     * @return the last previous ProductChangeAnnouncementEntity, if any
     */
    public Optional<ProductChangeAnnouncementEntity> findLastPrevious(
        ProductChangeAnnouncementEntity entity,
        List<ProductChangeAnnouncementEntity> otherProductChangeAnnouncements
    ) {
        return databaseService.runInThreadTransaction(session -> {
            var excludedIds = otherProductChangeAnnouncements.stream()
                .map(ProductChangeAnnouncementEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            excludedIds.add(entity.getId());

            var hql = """
                      FROM ProductChangeAnnouncementEntity
                      WHERE productEntity.id = :productId
                        AND channelProductListQuery IS NOT NULL
                        AND channelProductListQuery.channel.id = :channelId
                        AND id NOT IN :excludedIds
                        AND createdAt < :createdAt
                      ORDER BY createdAt DESC
                      """;
            return session.createQuery(hql, ProductChangeAnnouncementEntity.class)
                .setParameter("productId", entity.getProductEntity().getId())
                .setParameter("channelId", entity.getChannelProductListQuery().getChannel().getId())
                .setParameter("excludedIds", excludedIds)
                .setParameter("createdAt", entity.getCreatedAt())
                .setMaxResults(1)
                .uniqueResultOptional();
        });
    }
}
