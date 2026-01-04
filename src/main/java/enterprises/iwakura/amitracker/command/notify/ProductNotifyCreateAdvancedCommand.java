package enterprises.iwakura.amitracker.command.notify;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductNotifyCreateAdvancedCommand extends ProductNotifySubCommand {

    public ProductNotifyCreateAdvancedCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "create-advanced";
        this.help = "Create advanced product notification in current channel";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {

    }
}
