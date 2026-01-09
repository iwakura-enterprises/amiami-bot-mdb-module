package enterprises.iwakura.amitracker.database.repository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import enterprises.iwakura.amitracker.database.entity.WishlistEntryEntity;
import enterprises.iwakura.amitracker.object.Page;
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

    /**
     * Finds wishlist entries for a specific user and wishlist, paginated.
     *
     * @param userId     the ID of the user
     * @param wishlistId the ID of the wishlist
     * @param pageSize   the number of items per page
     * @param pageIndex  the index of the page to retrieve (0-based)
     *
     * @return a Page object containing the wishlist entries
     */
    public Page<WishlistEntryEntity> findWishlistEntriesPaged(
        long userId,
        long wishlistId,
        int pageSize,
        int pageIndex
    ) {
        return databaseService.runInThreadTransaction(session -> {
            String countHql = """
                SELECT COUNT(we.id)
                FROM WishlistEntryEntity we
                WHERE we.wishlist.id = :wishlistId AND we.wishlist.user.id = :userId
                """;
            long totalItems = Optional.ofNullable(session.createQuery(countHql, Long.class)
                .setParameter("wishlistId", wishlistId)
                .setParameter("userId", userId)
                .uniqueResult()).orElse(0L);

            String hql = """
                FROM WishlistEntryEntity we
                WHERE we.wishlist.id = :wishlistId AND we.wishlist.user.id = :userId
                ORDER BY we.createdAt DESC
                """;
            List<WishlistEntryEntity> items = session.createQuery(hql, WishlistEntryEntity.class)
                .setParameter("wishlistId", wishlistId)
                .setParameter("userId", userId)
                .setFirstResult(pageIndex * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

            int totalPages = totalItems > 0
                ? (int) Math.ceil((double) totalItems / pageSize)
                : 0;
            return new Page<>(items, pageSize, totalPages, totalItems);
        });
    }

    /**
     * Removes a wishlist entry for a specific user, wishlist name, and product code.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist
     * @param productCode  the code of the product
     *
     * @return true if the wishlist entry was removed, false otherwise
     */
    public boolean removeWishlistEntry(long userId, String wishlistName, String productCode) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                DELETE FROM WishlistEntryEntity we
                WHERE we.wishlist.user.id = :userId
                  AND LOWER(we.wishlist.name) = :wishlistName
                  AND we.product.code = :productCode
                """;
            int deletedCount = session.createQuery(hql, null)
                .setParameter("userId", userId)
                .setParameter("wishlistName", wishlistName.toLowerCase())
                .setParameter("productCode", productCode.toUpperCase())
                .executeUpdate();
            return deletedCount > 0;
        });
    }

    /**
     * Suggests products in a user's wishlist based on a search string.
     *
     * @param userId      the ID of the user
     * @param wishlistId  the ID of the wishlist
     * @param productCode the search string for product codes
     * @param maxElements the maximum number of results to return
     *
     * @return a list of matching WishlistEntryEntity
     */
    public List<WishlistEntryEntity> suggestProductsInWishlist(
        long userId,
        Long wishlistId,
        String productCode,
        int maxElements
    ) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                FROM WishlistEntryEntity we
                WHERE we.wishlist.user.id = :userId
                  AND we.wishlist.id = :wishlistId
                  AND we.product.code LIKE :searchPattern
                ORDER BY we.product.code ASC
                """;
            return session.createQuery(hql, WishlistEntryEntity.class)
                .setParameter("userId", userId)
                .setParameter("wishlistId", wishlistId)
                .setParameter("searchPattern", "%" + productCode.toUpperCase() + "%")
                .setMaxResults(maxElements)
                .getResultList();
        });
    }
}
