package enterprises.iwakura.amitracker.console;

import enterprises.iwakura.amitracker.constant.ImageRefreshReason;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.repository.ProductImageRefreshRepository;
import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.ganyu.annotation.Command;
import enterprises.iwakura.ganyu.annotation.DefaultCommand;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@RequiredArgsConstructor
@Command("create-missing-image-refreshments")
public class CreateMissingImageRefreshmentsConsoleCommand implements GanyuCommand {

    private final DatabaseService databaseService;
    private final ProductImageRefreshRepository productImageRefreshRepository;

    @DefaultCommand
    public void run() {
        log.info("Checking for products with no images...");
        var products = databaseService.runInThreadTransaction(session -> {
            var hql = """
            FROM ProductEntity p
            WHERE p.imageUrl IS NULL OR p.imageUrl = :noImageUrl
            """;
            return session.createQuery(hql, ProductEntity.class)
                .setParameter("noImageUrl", AmiAmiApiService.NO_IMAGE_URL)
                .getResultList();
        });

        log.info("Found {} products with no images, creating missing image refresh entries...", products.size());
        products.forEach(product -> productImageRefreshRepository.createPending(product, ImageRefreshReason.NO_IMAGE));
        log.info("Done.");
    }
}
