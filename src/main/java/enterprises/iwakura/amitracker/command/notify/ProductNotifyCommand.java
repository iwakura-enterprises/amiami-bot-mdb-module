package enterprises.iwakura.amitracker.command.notify;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductNotifyCommand extends AmiTrackerCommand {

    public ProductNotifyCommand(ConcurrencyService concurrencyService, List<ProductNotifySubCommand> subcommands) {
        super(concurrencyService);
        this.name = "product-notify";
        this.help = "Manage product notifications";

        this.children = subcommands.toArray(new ProductNotifySubCommand[0]);
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
    }
}
