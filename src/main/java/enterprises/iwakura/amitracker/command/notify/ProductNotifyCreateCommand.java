package enterprises.iwakura.amitracker.command.notify;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductNotifyCreateCommand extends ProductNotifySubCommand {

    public ProductNotifyCreateCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "create";
        this.help = "Create simple product notification in current channel";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {

    }
}
