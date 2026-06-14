package enterprises.iwakura.amitracker.command.notify;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProductNotifyTemplateCommand extends ProductNotifySubCommand {

    public ProductNotifyTemplateCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "template";
        this.help = "Creates a product search notification from a template";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {

    }
}
