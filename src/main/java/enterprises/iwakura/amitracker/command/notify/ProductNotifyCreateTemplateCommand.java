package enterprises.iwakura.amitracker.command.notify;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductNotifyCreateTemplateCommand extends ProductNotifySubCommand {

    public ProductNotifyCreateTemplateCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "create-template";
        this.help = "Create product notification from template in current channel";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {

    }
}
