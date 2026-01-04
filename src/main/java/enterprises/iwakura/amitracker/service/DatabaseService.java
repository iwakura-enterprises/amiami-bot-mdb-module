package enterprises.iwakura.amitracker.service;

import enterprises.iwakura.amitracker.AmiTracker;
import enterprises.iwakura.amitracker.database.repository.AmiBaseRepository;
import enterprises.iwakura.irminsul.IrminsulDatabaseService;
import enterprises.iwakura.sigewine.core.Sigewine;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class DatabaseService extends IrminsulDatabaseService {

    private final ConfigurationService configurationService;
    private final Sigewine sigewine;

    /**
     * Initializes the database service with entity classes.
     */
    public void initialize() {
        log.info("Initializing Hibernate...");
        databaseConfiguration = configurationService.getDatabase();

        var repositories = sigewine.getAllBeansThatAreAssignableFrom(AmiBaseRepository.class);
        log.info("Found {} repositories for entity mapping", repositories.size());

        super.initialize(AmiTracker.class.getClassLoader(),
            repositories.stream()
                .map(AmiBaseRepository::getEntityClass)
                .toArray(Class[]::new)
        );

        log.info("Running Liquibase migrations...");
        Thread.currentThread().setContextClassLoader(AmiTracker.class.getClassLoader());
        runLiquibase("liquibase/changelog.yaml", AmiTracker.class.getClassLoader());

        log.info("Database service initialized successfully");
    }
}
