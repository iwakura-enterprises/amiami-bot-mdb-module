package enterprises.iwakura.amitracker.command.wishlist;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.ProductService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.service.WishlistService;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class WishlistAddCommand extends WishlistSubCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";
    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    private final ProductService productService;
    private final WishlistService wishlistService;
    private final UserService userService;
    private final GuildService guildService;

    public WishlistAddCommand(ConcurrencyService concurrencyService, ProductService productService, WishlistService wishlistService,
        UserService userService,
        GuildService guildService
    ) {
        super(concurrencyService);
        this.productService = productService;
        this.wishlistService = wishlistService;
        this.userService = userService;
        this.guildService = guildService;
        this.name = "add";
        this.help = "Add product to your wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true, true),
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Shows specific wishlist", false, true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);
        var wishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);

        var hook = event.deferReply(true).complete();

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        wishlistService.ensureDefaultWishlistExists(user.getIdLong());
        handleProductAdd(user, hook, wishlistName, productCode);
    }

    public boolean handleProductAdd(User user, InteractionHook hook, String wishlistName, String productCode) {
        if (productCode == null || productCode.isBlank()) {
            hook.editOriginal("Product code is required").queue();
            return false;
        }

        productCode = URLHelper.extractProductCode(productCode, true);

        var errorContext = wishlistService.addProductToWishlist(user.getIdLong(), wishlistName, productCode);

        if (errorContext.isSuccess()) {
            hook.editOriginal("Product `%s` has been added to your wishlist `%s`.".formatted(productCode, wishlistName)).queue();
            return true;
        } else {
            switch (errorContext.getType()) {
                case PRODUCT_NOT_FOUND -> hook.editOriginal("Product `%s` was not found.".formatted(productCode)).queue();
                case WISHLIST_NOT_FOUND -> hook.editOriginal("Wishlist `%s` was not found.".formatted(wishlistName)).queue();
                case PRODUCT_ALREADY_IN_WISHLIST -> hook.editOriginal("Product `%s` is already in your wishlist `%s`.".formatted(
                    productCode, wishlistName)).queue();
                case WISHLIST_ENTRY_NOT_ADDED -> hook.editOriginal("Failed to add product `%s` to your wishlist `%s`.".formatted(
                    productCode, wishlistName)).queue();
                default -> hook.editOriginal("An unknown error occurred while adding product `%s` to your wishlist `%s`.".formatted(
                    productCode, wishlistName)).queue();
            }
            return false;
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var focusedOption = event.getFocusedOption().getName();
        var focusedValue = event.getFocusedOption().getValue();

        if (focusedOption.equals(OPTION_PRODUCT_CODE)) {
            event.replyChoices(productService.suggestProductCodes(focusedValue)).queue();
        } else if (focusedOption.equals(OPTION_WISHLIST_NAME)) {
            event.replyChoices(wishlistService.suggestWishlistNames(event.getUser().getIdLong(), focusedValue)).queue();
        }
    }
}
