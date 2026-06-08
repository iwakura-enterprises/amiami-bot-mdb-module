package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryResultEntryEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductQueryResultEntryRepository extends AmiBaseRepository<ProductListQueryResultEntryEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ProductQueryResultEntryRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<ProductListQueryResultEntryEntity> getEntityClass() {
        return ProductListQueryResultEntryEntity.class;
    }

    @Override
    protected boolean hasId(ProductListQueryResultEntryEntity productListQueryResultEntryEntity) {
        return productListQueryResultEntryEntity.getId() != null;
    }

    /**
     * Creates a result entry for the list query entity with the specified product entity
     *
     * @param productListQueryEntity Product list query entity
     * @param productEntity          Product entity
     *
     * @return Newely created {@link ProductListQueryResultEntryEntity} or existing one
     */
    public ProductListQueryResultEntryEntity createFor(
        ProductListQueryEntity productListQueryEntity,
        ProductEntity productEntity
    ) {
        return databaseService.runInThreadTransaction(session -> {
            var hql = """
                FROM ProductListQueryResultEntryEntity e
                WHERE e.productListQuery = :productListQuery AND e.product = :product
                """;

            var existing = session.createQuery(hql, ProductListQueryResultEntryEntity.class)
                .setParameter("productListQuery", productListQueryEntity)
                .setParameter("product", productEntity)
                .uniqueResultOptional();

            if (existing.isPresent()) {
                return existing.get();
            }

            var entryEntity = new ProductListQueryResultEntryEntity();
            entryEntity.setProductListQuery(productListQueryEntity);
            entryEntity.setProduct(productEntity);
            return save(entryEntity);
        });
    }
}
