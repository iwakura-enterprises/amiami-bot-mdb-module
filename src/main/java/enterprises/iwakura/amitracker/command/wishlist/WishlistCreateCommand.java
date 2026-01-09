package enterprises.iwakura.amitracker.command.wishlist;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.service.WishlistService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class WishlistCreateCommand extends WishlistSubCommand {

    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    private final UserService userService;
    private final GuildService guildService;
    private final WishlistService wishlistService;

    public WishlistCreateCommand(ConcurrencyService concurrencyService,
        UserService userService,
        GuildService guildService,
        WishlistService wishlistService
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.wishlistService = wishlistService;
        this.name = "create";
        this.help = "Create new wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Wishlist name", true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var wishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);

        var hook = event.deferReply(true).complete();

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        wishlistService.ensureDefaultWishlistExists(user.getIdLong());
        var errorContext = wishlistService.createWishlist(user.getIdLong(), wishlistName);

        if (errorContext.isSuccess()) {
            hook.editOriginal("Wishlist '" + wishlistName + "' created successfully!").queue();
        } else {
            switch (errorContext.getType()) {
                case WISHLIST_ALREADY_EXISTS -> hook.editOriginal("Wishlist '" + wishlistName + "' already exists!").queue();
                default -> hook.editOriginal("An unknown error occurred while creating the wishlist.").queue();
            }
        }
    }
}
