package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.BoughtProductEntity;
import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.irminsul.repository.BaseRepository;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class GuildRepository extends AmiBaseRepository<GuildEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public GuildRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<GuildEntity> getEntityClass() {
        return GuildEntity.class;
    }

    @Override
    protected boolean hasId(GuildEntity guildEntity) {
        return existsById(guildEntity.getId());
    }
}
