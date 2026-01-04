package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
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
}
