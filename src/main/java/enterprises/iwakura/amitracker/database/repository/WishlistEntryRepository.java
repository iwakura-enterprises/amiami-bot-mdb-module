package enterprises.iwakura.amitracker.database.repository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import enterprises.iwakura.amitracker.database.entity.WishlistEntryEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class WishlistEntryRepository extends AmiBaseRepository<WishlistEntryEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public WishlistEntryRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<WishlistEntryEntity> getEntityClass() {
        return WishlistEntryEntity.class;
    }

    @Override
    protected boolean hasId(WishlistEntryEntity wishlistEntryEntity) {
        return wishlistEntryEntity.getId() != null;
    }

    /**
     * Finds all pending wishlist entries whose associated products have not been updated within the specified interval.
     *
     * @param intervalMillis the interval in milliseconds
     *
     * @return a list of pending wishlist entries
     */
    public List<WishlistEntryEntity> findAllPending(long intervalMillis) {
        return databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                    FROM WishlistEntryEntity we
                    WHERE we.product.updatedAt IS NULL OR we.product.updatedAt <= :thresholdTime
                    """;
            var query = session.createQuery(hql, WishlistEntryEntity.class);
            query.setParameter("thresholdTime", OffsetDateTime.now().minus(intervalMillis, ChronoUnit.MILLIS));
            return query.list();
        });
    }
}
