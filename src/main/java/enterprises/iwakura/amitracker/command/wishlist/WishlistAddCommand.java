package enterprises.iwakura.amitracker.command.wishlist;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class WishlistAddCommand extends WishlistSubCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";
    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    public WishlistAddCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "add";
        this.help = "Add product to your wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true),
            // TODO: Autocomplete
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Shows specific wishlist", false)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);
        var wishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);

        if (productCode == null || productCode.isBlank()) {
            event.reply("Product code is required!").setEphemeral(true).queue();
            return;
        }

        event.reply("Wishlist add command executed: %s -> %s".formatted(productCode, wishlistName)).queue();
    }
}
