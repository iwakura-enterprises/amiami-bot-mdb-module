package enterprises.iwakura.amitracker.command.inventory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.CurrencyExchangeService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.InventoryService;
import enterprises.iwakura.amitracker.service.ProductService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
@Slf4j
public class InventoryAddCommand extends InventorySubCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";
    public static final String OPTION_PURCHASE_DATE = "purchase-date";
    public static final String OPTION_PRICE = "price";
    public static final String OPTION_CURRENCY = "currency";

    private final GuildService guildService;
    private final UserService userService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final CurrencyExchangeService currencyExchangeService;

    public InventoryAddCommand(
        ConcurrencyService concurrencyService,
        GuildService guildService, UserService userService, ProductService productService, InventoryService inventoryService,
        CurrencyExchangeService currencyExchangeService
    ) {
        super(concurrencyService);
        this.guildService = guildService;
        this.userService = userService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.currencyExchangeService = currencyExchangeService;
        this.name = "add";
        this.help = "Add product to your wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true, true),
            new OptionData(OptionType.NUMBER, OPTION_PRICE, "Price paid for the product (default in ¥JPY)", false),
            new OptionData(OptionType.STRING, OPTION_PURCHASE_DATE, "Purchase date (YYYY-MM-DD)", false),
            new OptionData(OptionType.STRING, OPTION_CURRENCY, "Currency", false).addChoices(Currency.CHOICES)
        );
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var focusedOption = event.getFocusedOption().getName();
        var focusedValue = event.getFocusedOption().getValue();
        if (focusedOption.equals(OPTION_PRODUCT_CODE)) {
            event.replyChoices(productService.suggestProductCodes(focusedValue)).queue();
        }
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);
        var price = event.getOption(OPTION_PRICE, OptionMapping::getAsDouble);
        var purchaseDateString = event.getOption(OPTION_PURCHASE_DATE, OptionMapping::getAsString);
        var purchaseDate = purchaseDateString == null ? OffsetDateTime.now()
            : OffsetDateTime.parse(purchaseDateString + "T00:00:00+00:00");
        var currencyString = event.getOption(OPTION_CURRENCY, Constants.DEFAULT_CURRENCY.name(),
            OptionMapping::getAsString);
        var currency = Currency.fromString(currencyString).orElse(null);

        var hook = event.deferReply(true).complete();

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        handleProductAdd(user, hook, productCode, purchaseDate, price, currency);
    }

    public boolean handleProductAdd(User user, InteractionHook hook, String productCode, OffsetDateTime purchaseDate, Double price, Currency currency) {
        if (productCode == null) {
            hook.editOriginal("Product code is required").queue();
            return false;
        }

        if (price != null && price < 0) {
            hook.editOriginal("Price must be a positive number").queue();
            return false;
        }

        if (currency == null) {
            hook.editOriginal("Invalid currency").queue();
            return false;
        }

        productCode = URLHelper.extractProductCode(productCode);

        if (inventoryService.hasUserBoughtProduct(user.getIdLong(), productCode)) {
            // TODO: Show interactive message to confirm update
            hook.editOriginal("You have already added product `" + productCode + "` to your inventory").queue();
            return false;
        }

        log.info("User {} adding product {} to inventory", user.getIdLong(), productCode);

        var optionalProduct = productService.getOrQueryProduct(productCode);

        if (optionalProduct.isEmpty()) {
            hook.editOriginal("Product with code `" + productCode + "` not found").queue();
            return false;
        }

        var product = optionalProduct.get();

        var priceJpy = Optional.ofNullable(currencyExchangeService.exchange(price, currency, Currency.JPY))
            .map(Math::round)
            .orElse(product.getPriceJpy());

        var boughtProduct = inventoryService.addBoughProduct(
            user.getIdLong(), product, purchaseDate, priceJpy, currency
        );

        hook.editOriginal("Added product `" + productCode + "` to your inventory with ID `" + boughtProduct.getId() + "`").queue();
        return true;
    }
}
