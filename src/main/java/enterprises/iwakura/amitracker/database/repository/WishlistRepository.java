package enterprises.iwakura.amitracker.database.repository;

import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.constant.ProductChangeType;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class WishlistRepository extends AmiBaseRepository<WishlistEntity, Long> {

    private final UserRepository userRepository;

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public WishlistRepository(DatabaseService databaseService, UserRepository userRepository) {
        super(databaseService);
        this.userRepository = userRepository;
    }

    @Override
    public Class<WishlistEntity> getEntityClass() {
        return WishlistEntity.class;
    }

    @Override
    protected boolean hasId(WishlistEntity wishlistEntity) {
        return wishlistEntity.getId() != null;
    }

    /**
     * Finds a wishlist by user ID and wishlist name.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist
     *
     * @return an Optional containing the WishlistEntity if found, or empty if not found
     */
    public Optional<WishlistEntity> findByUserIdAndName(long userId, String wishlistName) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                         FROM WishlistEntity w
                         WHERE w.user.id = :userId AND LOWER(w.name) = :wishlistName
                         """;
            return session.createQuery(hql, WishlistEntity.class)
                .setParameter("userId", userId)
                .setParameter("wishlistName", wishlistName.toLowerCase())
                .uniqueResultOptional();
        });
    }

    /**
     * Suggests wishlist names for a user based on a partial wishlist name.
     *
     * @param userId       the ID of the user
     * @param wishlistName the partial name of the wishlist
     *
     * @return a list of WishlistEntity matching the criteria
     */
    public List<WishlistEntity> suggestWishlistNamesForUser(long userId, String wishlistName) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                         FROM WishlistEntity w
                         WHERE w.user.id = :userId AND LOWER(w.name) LIKE :wishlistName
                         ORDER BY w.name ASC
                         """;
            return session.createQuery(hql, WishlistEntity.class)
                .setParameter("userId", userId)
                .setParameter("wishlistName", wishlistName.toLowerCase() + "%")
                .setMaxResults(25)
                .getResultList();
        });
    }

    /**
     * Ensures that a default wishlist exists for the specified user. If it does not exist, creates one.
     *
     * @param userId the ID of the user
     */
    public void ensureDefaultWishlistExistsForUser(long userId) {
        databaseService.runInThreadTransaction(session -> {
            var count = session.createQuery("""
                                            SELECT COUNT(w)
                                            FROM WishlistEntity w
                                            WHERE w.user.id = :userId AND LOWER(w.name) = :defaultName
                                            """, Long.class)
                .setParameter("userId", userId)
                .setParameter("defaultName", Constants.DEFAULT_WISHLIST_NAME.toLowerCase())
                .uniqueResult();

            if (count == null || count == 0) {
                var defaultWishlist = WishlistEntity.createDefault();
                defaultWishlist.setUser(userRepository.findById(userId).orElseThrow());
                save(defaultWishlist);
            }
        });
    }

    /**
     * Checks if a product is in the specified wishlist.
     *
     * @param wishlistId  the ID of the wishlist
     * @param productCode the code of the product
     *
     * @return true if the product is in the wishlist, false otherwise
     */
    public boolean isProductInWishlist(Long wishlistId, String productCode) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                         SELECT COUNT(we)
                         FROM WishlistEntryEntity we
                         WHERE we.wishlist.id = :wishlistId AND we.product.code = :productCode
                         """;
            Long count = session.createQuery(hql, Long.class)
                .setParameter("wishlistId", wishlistId)
                .setParameter("productCode", productCode.toUpperCase())
                .uniqueResult();
            return count != null && count > 0;
        });
    }

    /**
     * Creates a new wishlist for the specified user with the given name.
     *
     * @param userId       the ID of the user
     * @param wishlistName the name of the wishlist
     */
    public void createWishlist(long userId, String wishlistName) {
        databaseService.runInThreadTransaction(session -> {
            var wishlist = WishlistEntity.createDefault();
            wishlist.setName(wishlistName);
            var user = userRepository.findById(userId).orElseThrow();
            wishlist.setUser(user);
            save(wishlist);
        });
    }

    /**
     * Finds those wishlists that satisfies the product change types for specified produft
     *
     * @param productEntity      Product
     * @param productChangeTypes Product change types
     *
     * @return List of wishlists
     */
    public List<WishlistEntity> findWishlistsToNotify(
        ProductEntity productEntity,
        List<ProductChangeType> productChangeTypes
    ) {
        return databaseService.runInThreadTransaction(session -> {
            boolean checkPriceDiscount = productChangeTypes.contains(ProductChangeType.PRICE_DISCOUNT);
            boolean checkStockChange = productChangeTypes.contains(ProductChangeType.PRODUCT_STATE_CHANGED);

            if (!checkPriceDiscount && !checkStockChange) {
                return List.of();
            }

            var hql = """
                      SELECT DISTINCT w
                      FROM WishlistEntity w
                      JOIN w.entries e
                      WHERE e.product = :product
                      AND (
                          (:checkPriceDiscount = true AND w.priceDiscountEnabled = true)
                          OR
                          (:checkStockChange = true AND w.stockChangeEnabled = true)
                      )
                      """;

            return session.createQuery(hql, WishlistEntity.class)
                .setParameter("product", productEntity)
                .setParameter("checkPriceDiscount", checkPriceDiscount)
                .setParameter("checkStockChange", checkStockChange)
                .getResultList();
        });
    }

    /**
     * Counts all wishlists for user by ID
     *
     * @param userId User ID
     *
     * @return Number of wishlists that the user has
     */
    public long countForUserId(long userId) {
        return databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                FROM WishlistEntity w
                WHERE w.user.id = :userId
                """;
            return session.createQuery(hql, Long.class)
                .setParameter("userId", userId)
                .getResultCount();
        });
    }
}
