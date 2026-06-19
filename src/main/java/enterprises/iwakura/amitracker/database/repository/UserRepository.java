package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.UserEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    /**
     * Get or create a user by ID and name.
     *
     * @param id   User ID
     * @param name User name
     *
     * @return The existing or newly created UserEntity
     */
    public UserEntity getOrCreate(Long id, String name) {
        return databaseService.runInThreadTransaction(session -> {
            return findById(id).orElseGet(() -> {
                log.info("Creating UserEntity for {} ({})", id, name);
                UserEntity newUser = new UserEntity();
                newUser.setId(id);
                newUser.setName(name);
                return save(newUser);
            });
        });
    }

    /**
     * Updates user's name
     *
     * @param userId   User ID
     * @param userName User name
     */
    public void updateUserName(long userId, String userName) {
        databaseService.runInThreadTransaction(session -> {
            var hql =
                """
                UPDATE UserEntity u
                SET u.name = :userName
                WHERE u.id = :userId
                """;
            session.createQuery(hql)
                .setParameter("userId", userId)
                .setParameter("userName", userName)
                .executeUpdate();
            return null;
        });
    }
}
