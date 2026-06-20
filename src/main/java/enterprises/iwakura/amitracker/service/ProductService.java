package enterprises.iwakura.amitracker.service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.database.entity.ProductListQueryEntity;
import enterprises.iwakura.amitracker.database.entity.UserEntity;
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
    private final CurrencyExchangeService currencyExchangeService;

    private final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("#.##");

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
            return optionalProduct;
        } else {
            log.info("Product with code {} not found in database, scheduling an query", productCode);
            try {
                var response = amiAmiQueryService.scheduleItemDetail(new ProductQueryRequest(productCode)).join();
                return productProcessorService.process(response);
            } catch (CompletionException exception) {
                if (exception.getCause() instanceof QueryFailedException queryFailedException) {
                    log.warn("Failed to query product with code {}: {}", productCode,
                        queryFailedException.getMessage());
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
        var products = productRepository.suggestProductCodesFiltered(URLHelper.extractProductCode(searchQuery, true), OptionData.MAX_CHOICES);
        return products.stream()
            .map(product -> new ProductChoice(product).toChoice())
            .toList();
    }

    /**
     * Creates product info description for specified product
     *
     * @param guildEntity Guild Entity
     * @param userEntity User Entity
     * @param product Product
     * @param productListQueryEntity Product list query that is creating this description
     *
     * @return String containing some info
     */
    public String createProductInfoDescription(
        GuildEntity guildEntity,
        UserEntity userEntity,
        ProductEntity product,
        ProductListQueryEntity productListQueryEntity
    ) {
        var sb = new StringBuilder();
        var userPreferredCurrency = Optional.ofNullable(userEntity)
            .map(UserEntity::getPreferredCurrency);
        var guildPreferredCurrency = Optional.ofNullable(guildEntity)
            .map(GuildEntity::getPreferredCurrency);
        var preferredCurrency = userPreferredCurrency
            .or(() -> guildPreferredCurrency)
            .orElse(Currency.JPY);
        var secondaryCurrencies = Optional.ofNullable(guildEntity)
            .map(it -> new LinkedHashSet<>(it.getSecondaryCurrencies()))
            .orElseGet(LinkedHashSet::new);

        // If we're using user's preferred currency,
        if (userPreferredCurrency.isPresent()) {
            // remove it from secondary currencies from guild
            secondaryCurrencies.remove(userPreferredCurrency.get());

            // If guild has preferred currency, specify them in the secondary currencies
            if (guildPreferredCurrency.isPresent() && userPreferredCurrency.get() != guildPreferredCurrency.get()) {
                secondaryCurrencies.add(guildPreferredCurrency.get());
            }
        }

        sb.append("├  Price: **");
        var convertedPrice = currencyExchangeService.exchange(Double.valueOf(product.getPriceJpy()), Currency.JPY, preferredCurrency);
        if (preferredCurrency != Currency.JPY) {
            sb.append("~");
        }
        sb.append(CURRENCY_FORMATTER.format(convertedPrice))
                .append(" ")
                .append(preferredCurrency.getSymbol())
                .append("**");

        if (!secondaryCurrencies.isEmpty()) {
            sb.append(" (");
            secondaryCurrencies.forEach(currency -> {
                var price = currencyExchangeService.exchange(Double.valueOf(product.getPriceJpy()), Currency.JPY, currency);
                if (currency != Currency.JPY) {
                    sb.append("≈");
                }
                sb.append(CURRENCY_FORMATTER.format(price))
                    .append(" ")
                    .append(currency.getSymbol());
                sb.append(" / ");
            });
            // Remove trailing /
            sb.delete(sb.length() - 3, sb.length());
            sb.append(")");
        }

        if (preferredCurrency != Currency.JPY && !secondaryCurrencies.contains(Currency.JPY)) {
            sb.append(" (%s %s)".formatted(product.getPriceJpy(), Currency.JPY.getSymbol()));
        }

        sb.append("\n");
        sb.append("├  State: **%s**\n".formatted(product.getProductState()));
        sb.append("├  Maker: %s\n".formatted(Optional.ofNullable(product.getMakerName()).orElse("N/A")));
        sb.append("├  Release: %s\n".formatted(Optional.ofNullable(product.getReleaseDate()).map(LocalDate::toString).orElse("N/A")));

        if (productListQueryEntity == null) {
            sb.append("└  ");
        } else {
            sb.append("├  ");
        }

        sb.append("`%s`\n".formatted(product.getCode()));

        if (productListQueryEntity != null) {
            sb.append("└  [Search link](%s)".formatted(productListQueryEntity.getProductSearchParameters().toUrl()));
        }

        return sb.toString();
    }
}
