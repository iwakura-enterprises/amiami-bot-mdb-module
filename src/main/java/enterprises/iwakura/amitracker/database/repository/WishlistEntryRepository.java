package enterprises.iwakura.amitracker.database.repository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
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

    /**
     * Creates a new wishlist entry for the given wishlist and product if it does not already exist.
     *
     * @param wishlist the wishlist entity
     * @param product  the product entity
     *
     * @return an Optional containing the created WishlistEntryEntity, or the existing one if it already exists
     */
    public Optional<WishlistEntryEntity> createWishlistEntry(WishlistEntity wishlist, ProductEntity product) {
        return databaseService.runInThreadTransaction(session -> {
            // Check if the entry already exists
            String hql = """
                FROM WishlistEntryEntity we
                WHERE we.wishlist.id = :wishlistId AND we.product.id = :productId
                """;
            var query = session.createQuery(hql, WishlistEntryEntity.class);
            query.setParameter("wishlistId", wishlist.getId());
            query.setParameter("productId", product.getId());
            var existingEntry = query.uniqueResultOptional();
            if (existingEntry.isPresent()) {
                return existingEntry;
            }

            // Create new wishlist entry
            WishlistEntryEntity newEntry = new WishlistEntryEntity();
            newEntry.setWishlist(wishlist);
            newEntry.setProduct(product);
            save(newEntry);
            return Optional.of(newEntry);
        });
    }
}
