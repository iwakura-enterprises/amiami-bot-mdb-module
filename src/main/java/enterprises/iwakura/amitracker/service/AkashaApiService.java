package enterprises.iwakura.amitracker.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;

import enterprises.iwakura.kirara.akasha.AkashaApi;
import enterprises.iwakura.kirara.gson.GsonSerializer;
import enterprises.iwakura.kirara.httpclient.HttpClientHttpCore;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
public class AkashaApiService extends AkashaApi {

    private final ConfigurationService configurationService;

    public AkashaApiService(
        ConfigurationService configurationService,
        Gson gson
    ) {
        super(new HttpClientHttpCore(), new GsonSerializer(gson), null);
        this.configurationService = configurationService;
    }

    /**
     * Initializes the AkashaApiService
     */
    public void init() {
        log.info("Initializing AkashaApiService...");
        var akashaConfiguration = configurationService.getAkasha();

        apiUrl = akashaConfiguration.getUrl();
        defaultToken = akashaConfiguration.getToken();
    }

    /**
     * Writes a product image to Akasha and returns the URL of the uploaded image.
     *
     * @param productCode the product code
     * @param imageData   the image data as a byte array
     *
     * @return a CompletableFuture containing the URL of the uploaded image
     */
    public CompletableFuture<String> writeProductImage(String productCode, byte[] imageData) {
        var future = new CompletableFuture<String>();
        var akashaConfiguration = configurationService.getAkasha();

        var datasource = akashaConfiguration.getDatasource();
        var path = constructImageUploadPath(productCode);

        this.write(datasource, path, imageData)
            .send()
            .whenCompleteAsync((response, exception) -> {
                if (exception != null) {
                    log.error("Failed to upload image for product code {} to Akasha", productCode, exception);
                    future.completeExceptionally(exception);
                } else if (response != null && response.getStatus() != 200) {
                    log.error("Failed to upload image for product code {} to Akasha (response: {})",
                        productCode, response
                    );
                    future.completeExceptionally(new Exception("Failed to upload image to Akasha, status: " + response.getStatus()));
                } else {
                    log.debug("Uploaded image for product code {} to Akasha", productCode);
                    future.complete(constructImageUrl(productCode));
                }
            });

        return future;
    }

    /**
     * Reads a product image from Akasha.
     *
     * @param productCode the product code
     *
     * @return a CompletableFuture containing the image data as a byte array
     */
    public CompletableFuture<byte[]> readProductImage(String productCode, boolean logErrors) {
        var future = new CompletableFuture<byte[]>();
        var akashaConfiguration = configurationService.getAkasha();

        var datasource = akashaConfiguration.getDatasource();
        var path = constructImageUploadPath(productCode);

        this.read(datasource, path)
            .send()
            .whenCompleteAsync((response, exception) -> {
                if (exception != null) {
                    if (logErrors) {
                        log.error("Failed to read image for product code {} from Akasha", productCode, exception);
                    }
                    future.completeExceptionally(exception);
                } else if (response != null && response.getStatus() != 200) {
                    if (logErrors) {
                        log.error("Failed to read image for product code {} from Akasha (response: {})",
                            productCode, response
                        );
                    }
                    future.completeExceptionally(new Exception("Failed to read image from Akasha, status: " + response.getStatus()));
                } else if (response != null) {
                    log.debug("Read image for product code {} from Akasha", productCode);
                    future.complete(response.getContent());
                } else {
                    future.completeExceptionally(new Exception("Failed to read image from Akasha, response is null"));
                }
            });

        return future;
    }

    /**
     * Checks if a product image exists in Akasha.
     *
     * @param productCode the product code
     *
     * @return a CompletableFuture containing true if the image exists, false otherwise
     */
    public CompletableFuture<Boolean> existsProductImage(String productCode) {
        var future = new CompletableFuture<Boolean>();
        var akashaConfiguration = configurationService.getAkasha();

        var datasource = akashaConfiguration.getDatasource();
        var path = constructImageUploadPath(productCode);

        this.fileInfo(datasource, path)
            .send()
            .whenCompleteAsync((response, exception) -> {
                if (exception != null) {
                    log.error("Failed to check existence of image for product code {} from Akasha", productCode, exception);
                    future.completeExceptionally(exception);
                } else if (response != null && response.getStatus() != 200) {
                    log.error("Failed to check existence of image for product code {} from Akasha (response: {})",
                        productCode, response
                    );
                    future.completeExceptionally(new Exception("Failed to check existence of image from Akasha, status: " + response.getStatus()));
                } else if (response != null) {
                    log.debug("Checked existence of image for product code {} from Akasha", productCode);
                    future.complete(response.getFileInfo() != null);
                } else {
                    future.completeExceptionally(new Exception("Failed to check existence of image from Akasha, response is null"));
                }
            });

        return future;
    }

    /**
     * Constructs the full image URL for a given product code.
     *
     * @param productCode the product code
     *
     * @return the full image URL
     */
    public String constructImageUrl(String productCode) {
        var akashaConfiguration = configurationService.getAkasha();
        var datasource = akashaConfiguration.getDatasource();
        var path = String.format(akashaConfiguration.getProductImagePath(), productCode);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return "%s/data-source/%s/%s".formatted(
            akashaConfiguration.getUrl(),
            datasource,
            path
        );
    }

    private String constructImageUploadPath(String productCode) {
        var akashaConfiguration = configurationService.getAkasha();
        return String.format(akashaConfiguration.getProductImagePath(), productCode);
    }
}
