package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class WishlistRepository extends AmiBaseRepository<WishlistEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public WishlistRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<WishlistEntity> getEntityClass() {
        return WishlistEntity.class;
    }

    @Override
    protected boolean hasId(WishlistEntity wishlistEntity) {
        return wishlistEntity.getId() != null;
    }
}
