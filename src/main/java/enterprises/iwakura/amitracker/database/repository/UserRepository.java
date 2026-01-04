package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.database.entity.UserEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class UserRepository extends AmiBaseRepository<UserEntity, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public UserRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    @Override
    protected boolean hasId(UserEntity userEntity) {
        return existsById(userEntity.getId());
    }
}
