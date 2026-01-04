package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.ChannelEntity;
import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ChannelRepository extends AmiBaseRepository<ChannelEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ChannelRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<ChannelEntity> getEntityClass() {
        return ChannelEntity.class;
    }

    @Override
    protected boolean hasId(ChannelEntity channelEntity) {
        return existsById(channelEntity.getId());
    }
}
