package enterprises.iwakura.amitracker.database.repository;

import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.constant.Constants;
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
                WHERE w.user.id = :userId AND w.name = :wishlistName
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
                WHERE w.user.id = :userId AND w.name LIKE :wishlistName
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
                    WHERE w.user.id = :userId AND w.name = :defaultName
                    """, Long.class)
                .setParameter("userId", userId)
                .setParameter("defaultName", Constants.DEFAULT_WISHLIST_NAME)
                .uniqueResult();

            if (count == null || count == 0) {
                var defaultWishlist = WishlistEntity.createDefault();
                defaultWishlist.setUser(userRepository.findById(userId).orElseThrow());
                save(defaultWishlist);
            }
        });
    }
}
