package enterprises.iwakura.amitracker.command.wishlist;

import java.util.List;
import java.util.stream.Stream;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class WishlistEditCommand extends WishlistSubCommand {

    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    public WishlistEditCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "edit";
        this.help = "Edit your wishlist";

        this.options = List.of(
            // TODO: Autocomplete
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Choose specific wishlist", false, true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var queriedWishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);
        event.reply("You requested edit of the wishlist: " + queriedWishlistName).queue();
    }
}
