package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.irminsul.repository.BaseRepository;

public abstract class AmiBaseRepository<T, Long> extends BaseRepository<T, Long> {

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public AmiBaseRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    /**
     * Checks if an entity with the given ID exists in the database.
     *
     * @param id the ID to check
     *
     * @return true if an entity with the given ID exists, false otherwise
     */
    public boolean existsById(Long id) {
        return databaseService.runInThreadTransaction(session -> {
            String hql = "SELECT 1 FROM " + getEntityClass().getSimpleName() + " e WHERE e.id = :id";
            Object result = session.createQuery(hql, null)
                .setParameter("id", id)
                .setMaxResults(1)
                .uniqueResult();
            return result != null;
        });
    }
}
