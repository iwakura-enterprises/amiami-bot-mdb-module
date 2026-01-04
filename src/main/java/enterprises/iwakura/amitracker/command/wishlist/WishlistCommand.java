package enterprises.iwakura.amitracker.command.wishlist;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class WishlistCommand extends AmiTrackerCommand {

    public WishlistCommand(ConcurrencyService concurrencyService, List<WishlistSubCommand> subcommands) {
        super(concurrencyService);
        this.name = "wishlist";
        this.help = "Manage your wishlist";

        this.children = subcommands.toArray(new WishlistSubCommand[0]);
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
    }
}
