package enterprises.iwakura.amitracker.command.inventory;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class InventoryCommand extends AmiTrackerCommand {

    public InventoryCommand(ConcurrencyService concurrencyService, List<InventorySubCommand> subcommands) {
        super(concurrencyService);
        this.name = "inventory";
        this.help = "Manage your inventory";

        this.children = subcommands.toArray(new InventorySubCommand[0]);
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
    }
}
