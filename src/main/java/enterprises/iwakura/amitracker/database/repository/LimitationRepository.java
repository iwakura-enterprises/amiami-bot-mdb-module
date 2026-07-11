package enterprises.iwakura.amitracker.database.repository;

import java.util.Optional;

import enterprises.iwakura.amitracker.database.entity.LimitationEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class LimitationRepository extends AmiBaseRepository<LimitationEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public LimitationRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<LimitationEntity> getEntityClass() {
        return LimitationEntity.class;
    }

    @Override
    protected boolean hasId(LimitationEntity limitationEntity) {
        return limitationEntity.getId() != null;
    }

    public Optional<LimitationEntity> findByGuildId(long guildId) {
        return databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                FROM LimitationEntity  l
                WHERE l.guild.id = :guildId
                """;
            return session.createQuery(hql, LimitationEntity.class)
                .setParameter("guildId", guildId)
                .uniqueResultOptional();
        });
    }

    public Optional<LimitationEntity> findByUserId(long userId) {
        return databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                FROM LimitationEntity  l
                WHERE l.user.id = :userId
                """;
            return session.createQuery(hql, LimitationEntity.class)
                .setParameter("userId", userId)
                .uniqueResultOptional();
        });
    }
}
