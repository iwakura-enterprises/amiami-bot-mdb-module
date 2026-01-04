package enterprises.iwakura.amitracker.command.wishlist;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class WishlistDeleteCommand extends WishlistSubCommand {

    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    public WishlistDeleteCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "delete";
        this.help = "Delete your wishlist";

        this.options = List.of(
            // TODO: Autocomplete
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Choose specific wishlist", false, true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        // TODO
    }
}
