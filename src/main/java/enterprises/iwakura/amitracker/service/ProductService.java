package enterprises.iwakura.amitracker.service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.repository.ProductRepository;
import enterprises.iwakura.amitracker.exception.QueryFailedException;
import enterprises.iwakura.amitracker.object.ProductChoice;
import enterprises.iwakura.amitracker.objects.query.ProductQueryRequest;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AmiAmiQueryService amiAmiQueryService;
    private final ProductProcessorService productProcessorService;
    private final ProductImageService productImageService;

    /**
     * Get a product by its code, or query AmiAmi if not found.
     *
     * @param productCode the product code
     *
     * @return the product entity
     */
    public Optional<ProductEntity> getOrQueryProduct(String productCode) {
        var optionalProduct = productRepository.findByCode(productCode);

        if (optionalProduct.isPresent()) {
            var product = optionalProduct.get();

            if (product.getLastImageUpdateAt() == null) {
                // Schedule image fetching if not done before asynchronously
                productImageService.fetchImageUrl(product.getImageUrl());
            }

            return optionalProduct;
        } else {
            log.info("Product with code {} not found in database, scheduling an query", productCode);
            try {
                var response = amiAmiQueryService.scheduleItemDetail(new ProductQueryRequest(productCode)).join();
                return productProcessorService.process(response);
            } catch (CompletionException exception) {
                if (exception.getCause() instanceof QueryFailedException queryFailedException) {
                    log.warn("Failed to query product with code {}: {}", productCode, queryFailedException.getMessage());
                    return Optional.empty();
                }
                throw exception;
            }
        }
    }

    /**
     * Suggest product codes based on a search query.
     *
     * @param searchQuery the search query
     *
     * @return a collection of choices for product codes
     */
    public Collection<Choice> suggestProductCodes(String searchQuery) {
        var products = productRepository.suggestProductCodesFiltered(URLHelper.extractProductCode(searchQuery), OptionData.MAX_CHOICE_NAME_LENGTH);
        return products.stream()
            .map(product -> new ProductChoice(product).toChoice())
            .toList();
    }
}
