package enterprises.iwakura.amitracker.database.repository;

import java.util.List;

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

            if (!checkPriceDiscount && !checkStockChange && !checkNewProducts) {
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
                      )
                      """;

            return session.createQuery(hql, ChannelProductListQueryEntity.class)
                .setParameter("product", productEntity)
                .setParameter("productListQueryEntity", productListQueryEntity)
                .setParameter("checkPriceDiscount", checkPriceDiscount)
                .setParameter("checkStockChange", checkStockChange)
                .setParameter("checkNewProducts", checkNewProducts)
                .getResultList();
        });
    }
}
