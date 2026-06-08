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
}
