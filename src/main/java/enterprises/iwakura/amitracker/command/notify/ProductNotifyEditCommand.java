package enterprises.iwakura.amitracker.command.notify;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class ProductNotifyEditCommand extends ProductNotifySubCommand {

    public static final String OPTION_PRODUCT_LIST_NAME = "product-list-name";

    public ProductNotifyEditCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "edit";
        this.help = "Edit product search notification settings in current server";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_PRODUCT_LIST_NAME, "Specific product notifications", true, true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {

    }
}
