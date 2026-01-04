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
public class InventoryRemoveCommand extends InventorySubCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";

    public InventoryRemoveCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "remove";
        this.help = "Remove product from your wishlist";

        this.options = List.of(
            // TODO: Autocomplete
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);

        event.reply("Removing product '%s' from your inventory...".formatted(productCode)).queue();
    }
}
