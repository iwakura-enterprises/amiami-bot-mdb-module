package enterprises.iwakura.amitracker.database.repository;

import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.BoughtProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.object.Page;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class BoughtProductRepository extends AmiBaseRepository<BoughtProductEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public BoughtProductRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<BoughtProductEntity> getEntityClass() {
        return BoughtProductEntity.class;
    }

    @Override
    protected boolean hasId(BoughtProductEntity boughtProductEntity) {
        return boughtProductEntity.getId() != null;
    }

    /**
     * Finds all bought products for a given user ID.
     *
     * @param userId the ID of the user
     *
     * @return list of BoughtProductEntity associated with the user
     */
    public List<BoughtProductEntity> findByUserId(long userId) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = "FROM BoughtProductEntity WHERE user.id = :userId";
            return session.createQuery(hql, BoughtProductEntity.class)
                .setParameter("userId", userId)
                .getResultList();
        });
    }

    /**
     * Checks if a user has bought a specific product.
     *
     * @param userId      User ID
     * @param productCode Product code
     *
     * @return true if the user has bought the product, false otherwise
     */
    public boolean hasUserBoughtProduct(long userId, String productCode) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                SELECT COUNT(bp)
                FROM BoughtProductEntity bp
                WHERE bp.user.id = :userId
                  AND bp.product.code = :productCode
                """;
            Long count = session.createQuery(hql, Long.class)
                .setParameter("userId", userId)
                .setParameter("productCode", productCode.toUpperCase())
                .uniqueResult();
            return count != null && count > 0;
        });
    }

    /**
     * Finds bought product codes for a user filtered by a search pattern.
     *
     * @param userId             the ID of the user
     * @param searchingProductCode the search pattern for product codes
     * @param maxElements        maximum number of results to return
     *
     * @return list of product entities matching the search pattern
     */
    public List<ProductEntity> findBoughtProductCodesFiltered(long userId, String searchingProductCode, int maxElements) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                SELECT bp.product
                FROM BoughtProductEntity bp
                WHERE bp.user.id = :userId AND bp.product.code LIKE :searchPattern
                ORDER BY bp.product.code ASC
                """;
            return session.createQuery(hql, ProductEntity.class)
                .setParameter("userId", userId)
                .setParameter("searchPattern", "%" + searchingProductCode.toUpperCase() + "%")
                .setMaxResults(maxElements)
                .getResultList();
        });
    }

    /**
     * Removes a bought product for a user.
     *
     * @param userId      User ID
     * @param productCode Product code
     *
     * @return true if the product was removed, false otherwise
     */
    public boolean removeBoughtProduct(long userId, String productCode) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = """
                DELETE FROM BoughtProductEntity bp
                WHERE bp.user.id = :userId AND bp.product.code = :productCode
                """;
            int deletedCount = session.createQuery(hql, null)
                .setParameter("userId", userId)
                .setParameter("productCode", productCode.toUpperCase())
                .executeUpdate();
            return deletedCount > 0;
        });
    }

    /**
     * Finds bought products for a user with pagination.
     *
     * @param userId    the ID of the user
     * @param pageSize  number of items per page
     * @param pageIndex index of the page to retrieve
     *
     * @return a Page object containing BoughtProductEntity items
     */
    public Page<BoughtProductEntity> findBoughtProductsPaged(long userId, int pageSize, int pageIndex) {
        return databaseService.runInThreadTransaction(session -> {
            String countHql = "SELECT COUNT(bp) FROM BoughtProductEntity bp WHERE bp.user.id = :userId";
            long totalItems = Optional.ofNullable(session.createQuery(countHql, Long.class)
                .setParameter("userId", userId)
                .uniqueResult()).orElse(0L);

            String hql = "FROM BoughtProductEntity bp WHERE bp.user.id = :userId ORDER BY bp.boughtAt DESC";
            List<BoughtProductEntity> items = session.createQuery(hql, BoughtProductEntity.class)
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
}
