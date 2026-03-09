package enterprises.iwakura.amitracker.command.wishlist;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.service.WishlistService;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class WishlistRemoveCommand extends WishlistSubCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";
    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    private final UserService userService;
    private final GuildService guildService;
    private final WishlistService wishlistService;

    public WishlistRemoveCommand(ConcurrencyService concurrencyService,
        UserService userService,
        GuildService guildService,
        WishlistService wishlistService
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.wishlistService = wishlistService;
        this.name = "remove";
        this.help = "Remove product to your wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true),
        new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Choose specific wishlist", false)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);
        var wishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);

        var hook = event.deferReply(true).complete();

        if (productCode == null || productCode.isBlank()) {
            hook.editOriginal("Product code is required").queue();
            return;
        }

        productCode = URLHelper.extractProductCode(productCode);

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        wishlistService.ensureDefaultWishlistExists(user.getIdLong());
        var removed = wishlistService.removeProductFromWishlist(user.getIdLong(), wishlistName, productCode);

        if (removed) {
            hook.editOriginal("Product '" + productCode + "' removed from wishlist '" + wishlistName + "' successfully!").queue();
        } else {
            hook.editOriginal("Product '" + productCode + "' not found in wishlist '" + wishlistName + "'.").queue();
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var user = event.getUser();
        var focusedOption = event.getFocusedOption().getName();
        var focusedValue = event.getFocusedOption().getValue();
        var wishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);

        if (focusedOption.equals(OPTION_PRODUCT_CODE)) {
            event.replyChoices(wishlistService.suggestProductCodesInWishlist(user.getIdLong(), wishlistName, focusedValue)).queue();
        } else if (focusedOption.equals(OPTION_WISHLIST_NAME)) {
            event.replyChoices(wishlistService.suggestWishlistNames(user.getIdLong(), focusedValue)).queue();
        }
    }
}
