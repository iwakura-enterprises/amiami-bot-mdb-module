package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    /**
     * Get or create a guild by ID and name.
     *
     * @param guildId Guild ID
     * @param name    Guild name
     *
     * @return The existing or newly created GuildEntity
     */
    public GuildEntity getOrCreate(long guildId, String name) {
        return databaseService.runInThreadTransaction(session -> {
            return findById(guildId).orElseGet(() -> {
                log.info("Creating GuildEntity for {} ({})", guildId, name);
                GuildEntity newGuild = new GuildEntity();
                newGuild.setId(guildId);
                newGuild.setName(name);
                return save(newGuild);
            });
        });
    }

    /**
     * Updates guild's name
     *
     * @param guildId   Guild ID
     * @param guildName Guild name
     */
    public void updateGuildName(long guildId, String guildName) {
        databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                UPDATE GuildEntity g
                SET g.name = :guildName
                WHERE g.id = :guildId
                """;
            session.createQuery(hql, null)
                .setParameter("guildId", guildId)
                .setParameter("guildName", guildName)
                .executeUpdate();
            return null;
        });
    }
}
