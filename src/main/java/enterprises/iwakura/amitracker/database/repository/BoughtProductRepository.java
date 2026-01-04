package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.BoughtProductEntity;
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
}
