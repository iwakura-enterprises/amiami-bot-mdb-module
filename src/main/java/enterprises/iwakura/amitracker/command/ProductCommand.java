package enterprises.iwakura.amitracker.command;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class ProductCommand extends AmiTrackerCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";

    public ProductCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "product";
        this.help = "Get product information";

        this.options = List.of(
            // TODO: Autocomplete
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        String productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);

        event.reply("Product code: " + productCode).queue();
    }
}
