package enterprises.iwakura.amitracker.database.repository;

import java.util.List;
import java.util.Optional;

import enterprises.iwakura.amitracker.constant.ProductChangeType;
import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ChannelListProductQueryRepository extends AmiBaseRepository<ChannelProductListQueryEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ChannelListProductQueryRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<ChannelProductListQueryEntity> getEntityClass() {
        return ChannelProductListQueryEntity.class;
    }

    @Override
    protected boolean hasId(ChannelProductListQueryEntity channelProductListQueryEntity) {
        return channelProductListQueryEntity.getId() != null;
    }

    /**
     * Finds those channel product list query entities that have channel list query with entry of specified product
     * and that channel product list query specifies filters that satisfy the product change types. Additionally
     * filters by productListQueryEntity
     *
     * @param productEntity          Product entity
     * @param productListQueryEntity product list query
     * @param productChangeTypes     Product change types
     *
     * @return List of ChannelProductListQueryEntity
     */
    public List<ChannelProductListQueryEntity> findEntriesToNotify(
        ProductEntity productEntity,
        ProductListQueryEntity productListQueryEntity,
        List<ProductChangeType> productChangeTypes
    ) {
        return databaseService.runInThreadTransaction(session -> {
            boolean checkPriceDiscount = productChangeTypes.contains(ProductChangeType.PRICE_DISCOUNT);
            boolean checkStockChange = productChangeTypes.contains(ProductChangeType.PRODUCT_STATE_CHANGED);
            boolean checkNewProducts = productChangeTypes.contains(ProductChangeType.PRODUCT_LIST_NEW_PRODUCT);
            boolean experimentalImageUrlChanged = productChangeTypes.contains(ProductChangeType.EXPERIMENTAL_IMAGE_URL_CHANGE);

            if (!checkPriceDiscount && !checkStockChange && !checkNewProducts && !experimentalImageUrlChanged) {
                return List.of();
            }

            var hql = """
                      SELECT DISTINCT c
                      FROM ChannelProductListQueryEntity c
                      JOIN c.productListQuery pq
                      JOIN pq.entries e
                      WHERE e.product = :product AND (:productListQueryEntity is null or c.productListQuery = :productListQueryEntity)
                      AND (
                          (:checkPriceDiscount = true AND c.priceDiscountEnabled = true)
                          OR
                          (:checkStockChange = true AND c.stockChangeEnabled = true)
                          OR
                          (:checkNewProducts = true AND c.newProductsEnabled = true)
                          OR
                          (:experimentalImageUrlChanged = true)
                      )
                      """;

            return session.createQuery(hql, ChannelProductListQueryEntity.class)
                .setParameter("product", productEntity)
                .setParameter("productListQueryEntity", productListQueryEntity)
                .setParameter("checkPriceDiscount", checkPriceDiscount)
                .setParameter("checkStockChange", checkStockChange)
                .setParameter("checkNewProducts", checkNewProducts)
                .setParameter("experimentalImageUrlChanged", experimentalImageUrlChanged)
                .getResultList();
        });
    }

    /**
     * Finds all {@link ChannelProductListQueryEntity} by their channel ID
     *
     * @param channelId Channel ID
     *
     * @return List of ChannelProductListQueryEntity
     */
    public List<ChannelProductListQueryEntity> findAllByChannelId(long channelId) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      FROM ChannelProductListQueryEntity c
                      WHERE c.channel.id = :channelId
                      """;

            return session.createQuery(hql, ChannelProductListQueryEntity.class)
                .setParameter("channelId", channelId)
                .getResultList();
        });
    }

    /**
     * Finds a {@link ChannelProductListQueryEntity} by channel ID and name (case-insensitive)
     *
     * @param channelId Channel ID
     * @param name      Name
     *
     * @return Optional of ChannelProductListQueryEntity
     */
    public Optional<ChannelProductListQueryEntity> findByChannelIdAndName(long channelId, String name) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                  FROM ChannelProductListQueryEntity c
                  WHERE c.channel.id = :channelId
                  AND LOWER(c.name) = LOWER(:name)
                  """;

            return session.createQuery(hql, ChannelProductListQueryEntity.class)
                .setParameter("channelId", channelId)
                .setParameter("name", name)
                .uniqueResultOptional();
        });
    }

    /**
     * Finds all {@link ChannelProductListQueryEntity} by their guild ID
     *
     * @param guildId Guild ID
     *
     * @return list of ChannelProductListQueryEntity
     */
    public List<ChannelProductListQueryEntity> findAllByGuildId(long guildId) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                      FROM ChannelProductListQueryEntity c
                      WHERE c.channel.guild.id = :guildId
                      """;

            return session.createQuery(hql, ChannelProductListQueryEntity.class)
                .setParameter("guildId", guildId)
                .getResultList();
        });
    }

    /**
     * Suggests ChannelProductListQueryEntities based on the guild ID and searching name
     *
     * @param guildId       Guild ID
     * @param searchingName searching name
     * @param maxElements   Max elements
     *
     * @return List of resulting ChannelProductListQueryEntities
     */
    public List<ChannelProductListQueryEntity> suggestByGuild(long guildId, String searchingName, int maxElements) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                  FROM ChannelProductListQueryEntity c
                  WHERE c.channel.guild.id = :guildId
                  AND c.name LIKE :searchingName
                  ORDER BY c.name ASC
                  """;

            return session.createQuery(hql, ChannelProductListQueryEntity.class)
                .setParameter("guildId", guildId)
                .setParameter("searchingName", "%" + searchingName + "%")
                .setMaxResults(maxElements)
                .getResultList();
        });
    }

    /**
     * Finds ChannelProductListQueryEntity by guild ID and entity ID. Makes sure the entity is from the
     * specified guild ID.
     *
     * @param guildId  Guild ID
     * @param entityId Entity ID
     *
     * @return Optional of ChannelProductListQueryEntity
     */
    public Optional<ChannelProductListQueryEntity> findByGuildIdAndId(long guildId, long entityId) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                  FROM ChannelProductListQueryEntity c
                  WHERE c.id = :entityId
                  AND c.channel.guild.id = :guildId
                  """;

            return session.createQuery(hql, ChannelProductListQueryEntity.class)
                .setParameter("entityId", entityId)
                .setParameter("guildId", guildId)
                .uniqueResultOptional();
        });
    }
}
