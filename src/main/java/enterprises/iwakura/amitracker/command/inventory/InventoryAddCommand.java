package enterprises.iwakura.amitracker.command.inventory;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.wishlist.WishlistSubCommand;
import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class InventoryAddCommand extends InventorySubCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";
    public static final String OPTION_PURCHASE_DATE = "purchase-date";
    public static final String OPTION_PRICE = "price";
    public static final String OPTION_CURRENCY = "currency";

    public InventoryAddCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "add";
        this.help = "Add product to your wishlist";

        this.options = List.of(
            // TODO: Autocomplete
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true),
            new OptionData(OptionType.NUMBER, OPTION_PRICE, "Price paid for the product (default in ¥JPY)", true),
            new OptionData(OptionType.STRING, OPTION_PURCHASE_DATE, "Purchase date (YYYY-MM-DD)", false),
            new OptionData(OptionType.STRING, OPTION_CURRENCY, "Currency", false).addChoices(Currency.CHOICES)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);
        var purchaseDate = event.getOption(OPTION_PURCHASE_DATE, OptionMapping::getAsString);
        var price = event.getOption(OPTION_PRICE, OptionMapping::getAsDouble);
        var currency = event.getOption(OPTION_CURRENCY, Constants.DEFAULT_CURRENCY.name(), OptionMapping::getAsString);

        event.reply("Adding product `%s` to inventory, date %s, price %s %s".formatted(
            productCode, purchaseDate, price, currency
        )).queue();
    }
}
