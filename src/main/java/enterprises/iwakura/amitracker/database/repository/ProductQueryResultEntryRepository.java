package enterprises.iwakura.amitracker.database.repository;

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
}
