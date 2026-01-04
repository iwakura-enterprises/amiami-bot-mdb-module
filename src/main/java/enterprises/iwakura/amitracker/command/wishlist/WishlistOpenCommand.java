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
public class WishlistOpenCommand extends WishlistSubCommand {

    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    public WishlistOpenCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "open";
        this.help = "Open your wishlist";

        this.options = List.of(
            // TODO: Autocomplete
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Choose specific wishlist", false, true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var queriedWishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);
        event.reply("You requested the wishlist: " + queriedWishlistName).queue();
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var queriedWishlistName = event.getFocusedOption().getValue();

        // TODO: Actually fetch wishlist names from database
        event.replyChoices(Stream.of(
            new Choice("Example Wishlist", "ExampleWishlist"),
            new Choice("Another Wishlist", "AnotherWishlist"),
            new Choice("My wishlist lmao", "Mywishlistlmao"),
            new Choice("fumos", "fumos"),
            new Choice("characters", "whatever"),
            new Choice("a", "a"),
            new Choice("b", "b"),
            new Choice("c", "c"),
            new Choice("abcd", "abcd")
        ).filter(choice -> choice.getName().contains(queriedWishlistName)).toList()).queue();
    }
}
