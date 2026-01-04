package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.ProductHistoryEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductHistoryRepository extends AmiBaseRepository<ProductHistoryEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ProductHistoryRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<ProductHistoryEntity> getEntityClass() {
        return ProductHistoryEntity.class;
    }

    @Override
    protected boolean hasId(ProductHistoryEntity productHistoryEntity) {
        return productHistoryEntity.getId() != null;
    }
}
