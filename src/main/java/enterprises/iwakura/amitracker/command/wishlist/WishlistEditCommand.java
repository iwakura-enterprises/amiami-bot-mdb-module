package enterprises.iwakura.amitracker.command.wishlist;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.service.WishlistService;
import enterprises.iwakura.amitracker.util.StringUtils;
import enterprises.iwakura.jdainteractables.Interaction;
import enterprises.iwakura.jdainteractables.InteractionHandler.Result;
import enterprises.iwakura.jdainteractables.InteractionRules;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
public class WishlistEditCommand extends WishlistSubCommand {

    public static final String OPTION_WISHLIST_NAME = "wishlist-name";

    private final UserService userService;
    private final GuildService guildService;
    private final WishlistService wishlistService;
    private final WishlistDeleteCommand wishlistDeleteCommand;

    // Prevent circular dependency
    @Bean
    private final BeanAccessor<WishlistOpenCommand> wishlistOpenCommandAccessor = new BeanAccessor<>(WishlistOpenCommand.class);

    public WishlistEditCommand(ConcurrencyService concurrencyService,
        UserService userService,
        GuildService guildService,
        WishlistService wishlistService, WishlistDeleteCommand wishlistDeleteCommand
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.wishlistService = wishlistService;
        this.wishlistDeleteCommand = wishlistDeleteCommand;
        this.name = "edit";
        this.help = "Edit your wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Choose specific wishlist", true, true)
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

        var optionalWishlist = wishlistService.findWishlistForUser(user.getIdLong(), wishlistName);
        if (optionalWishlist.isPresent()) {
            showMainMenu(user, hook, optionalWishlist.get());
        } else {
            hook.editOriginal("Wishlist '" + wishlistName + "' not found.").queue();
        }
    }

    public void showMainMenu(User user, InteractionHook hook, WishlistEntity wishlist) {
        var interactableMessage = new InteractableMessage();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user)); // Only allow the command user to interact
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();
        components.add(TextDisplay.of("### Editing wishlist %s".formatted(StringUtils.capitalize(wishlist.getName()))));
        components.add(Separator.createDivider(Spacing.SMALL));

        var priceDiscountButton = interactableMessage.addInteraction(Interaction.asButton(createToggleButton(wishlist.getPriceDiscountEnabled())), event -> {
            event.deferEdit().complete();

            boolean newState = !wishlist.getPriceDiscountEnabled();
            wishlist.setPriceDiscountEnabled(newState);
            wishlistService.saveWishlist(wishlist);
            showMainMenu(user, hook, wishlist);

            return Result.REMOVE;
        });

        var stockChangeButton = interactableMessage.addInteraction(Interaction.asButton(createToggleButton(wishlist.getStockChangeEnabled())), event -> {
            event.deferEdit().complete();

            boolean newState = !wishlist.getStockChangeEnabled();
            wishlist.setStockChangeEnabled(newState);
            wishlistService.saveWishlist(wishlist);
            showMainMenu(user, hook, wishlist);

            return Result.REMOVE;
        });

        // Main body
        var sections = List.of(
            Section.of(
                priceDiscountButton,
                TextDisplay.of("**Price Discount Alerts**"),
                TextDisplay.of("Enables or disables price discount alerts for this wishlist.\nWhen enabled, you will receive notifications **when products in this wishlist have price drops**.")
            ),
            Section.of(
                stockChangeButton,
                TextDisplay.of("**Stock Change Alerts**"),
                TextDisplay.of("Enables or disables stock change alerts for this wishlist.\nWhen enabled, you will receive notifications **when products in this wishlist come back in stock**.")
            )
        );

        components.addAll(sections);
        components.add(Separator.createDivider(Spacing.SMALL));

        // Buttons
        var buttons = new ArrayList<Button>();
        var showButton = interactableMessage.addInteraction(Interaction.asButton(Button.secondary("abc", "Show Wishlist")), event -> {
            var buttonHook = event.deferEdit().complete();
            var openCommand = wishlistOpenCommandAccessor.getBeanInstance();
            openCommand.show(user, buttonHook, wishlist, 0, false);
            return Result.REMOVE;
        });
        buttons.add(showButton);

        var deleteButton = interactableMessage.addInteraction(Interaction.asButton(Button.danger("abc", "Delete Wishlist")), event -> {
            var buttonHook = event.deferReply(true).complete();

            var success = wishlistDeleteCommand.handleDelete(user, buttonHook, wishlist.getName());

            if (success) {
                event.getMessage().delete().queue();
            } else {
                // Show the main menu again
                showMainMenu(user, hook, wishlist);
            }

            return Result.REMOVE;
        });
        buttons.add(deleteButton);
        components.add(ActionRow.of(buttons));

        var container = Container.of(components);
        messageBuilder.setComponents(container);
        hook.editOriginal(messageBuilder.build()).queue(interactableMessage.registerOnCompleted());
    }

    private Button createToggleButton(boolean currentlyEnabled) {
        if (currentlyEnabled) {
            return Button.primary("abc", "Enabled");
        } else {
            return Button.secondary("abc", "Disabled");
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var focusedOption = event.getFocusedOption().getName();
        var focusedValue = event.getFocusedOption().getValue();

        if (focusedOption.equals(OPTION_WISHLIST_NAME)) {
            event.replyChoices(wishlistService.suggestWishlistNames(event.getUser().getIdLong(), focusedValue)).queue();
        }
    }
}
