package enterprises.iwakura.amitracker.command.wishlist;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.ProductImageService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.service.WishlistService;
import enterprises.iwakura.amitracker.util.ModalUtils;
import enterprises.iwakura.amitracker.util.NumberUtils;
import enterprises.iwakura.amitracker.util.StringUtils;
import enterprises.iwakura.jdainteractables.Interaction;
import enterprises.iwakura.jdainteractables.InteractionHandler.Result;
import enterprises.iwakura.jdainteractables.InteractionRules;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.jdainteractables.components.InteractableModal;
import enterprises.iwakura.kirara.amiami.AmiAmiApi;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
public class WishlistOpenCommand extends WishlistSubCommand {

    public static final String OPTION_WISHLIST_NAME = "wishlist-name";
    public static final String OPTION_DELETE_MODE_NAME = "delete-mode";
    public static final String MODAL_INPUT_PRODUCT_CODE = "product-code";
    public static final int PAGE_SIZE = 5;

    private final WishlistService wishlistService;
    private final UserService userService;
    private final GuildService guildService;
    private final ProductImageService productImageService;
    private final AmiAmiApiService amiAmiApiService;
    private final WishlistAddCommand wishlistAddCommand;
    private final WishlistEditCommand wishlistEditCommand;

    public WishlistOpenCommand(ConcurrencyService concurrencyService, WishlistService wishlistService,
        UserService userService,
        GuildService guildService, ProductImageService productImageService, AmiAmiApiService amiAmiApiService,
        WishlistAddCommand wishlistAddCommand, WishlistEditCommand wishlistEditCommand
    ) {
        super(concurrencyService);
        this.wishlistService = wishlistService;
        this.userService = userService;
        this.guildService = guildService;
        this.productImageService = productImageService;
        this.amiAmiApiService = amiAmiApiService;
        this.wishlistAddCommand = wishlistAddCommand;
        this.wishlistEditCommand = wishlistEditCommand;

        this.name = "open";
        this.help = "Open your wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_WISHLIST_NAME, "Choose specific wishlist", false, true),
            new OptionData(OptionType.BOOLEAN, OPTION_DELETE_MODE_NAME, "Enable delete mode", false)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var queriedWishlistName = event.getOption(OPTION_WISHLIST_NAME, Constants.DEFAULT_WISHLIST_NAME, OptionMapping::getAsString);
        var deleteMode = event.getOption(OPTION_DELETE_MODE_NAME, false, OptionMapping::getAsBoolean);
        var user = event.getUser();
        var guild = event.getGuild();

        var hook = event.deferReply().complete();

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        wishlistService.ensureDefaultWishlistExists(user.getIdLong());
        var optionalWishlist = wishlistService.findWishlistForUser(user.getIdLong(), queriedWishlistName);

        if (optionalWishlist.isEmpty()) {
            hook.editOriginal("Wishlist '%s' not found.".formatted(queriedWishlistName)).queue();
        } else {
            show(user, hook, optionalWishlist.get(), 0, deleteMode);
        }
    }

    public void show(User user, InteractionHook hook, WishlistEntity wishlist, int pageIndex, boolean deleteMode) {
        var interactableMessage = new InteractableMessage();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user)); // Only allow the command user to interact
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();
        components.add(TextDisplay.of("### %s wishlist".formatted(StringUtils.capitalize(wishlist.getName()))));
        components.add(Separator.createDivider(Spacing.SMALL));

        var page = wishlistService.getInventoryPage(user.getIdLong(), wishlist.getId(), PAGE_SIZE, pageIndex);

        if (page.getTotalElements() == 0) {
            components.add(TextDisplay.of("This wishlist is empty."));
        } else {
            // Main sections for each wishlist entry
            var sections = page.getContent()
                .stream()
                .map(entry -> {
                    var product = entry.getProduct();
                    SectionAccessoryComponent accessoryComponent;

                    if (deleteMode) {
                        accessoryComponent = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                            ButtonStyle.DANGER, "abcd", "Delete Product"
                        )), event -> {
                            event.deferEdit().complete();
                            wishlistService.removeProductFromWishlist(user.getIdLong(), wishlist.getName(), product.getCode());

                            // If user deleted the last item on the page, go back one page
                            if (page.getContent().size() == 1) {
                                show(user, hook, wishlist, Math.max(0, pageIndex - 1), true);
                            } else {
                                show(user, hook, wishlist, pageIndex, true);
                            }

                            return Result.REMOVE;
                        });
                    } else {
                        try {
                            var imageUrl = productImageService.fetchImageUrl(product.getImageUrl()).join();
                            accessoryComponent = Thumbnail.fromUrl(imageUrl);
                        } catch (Exception exception) {
                            accessoryComponent = Thumbnail.fromUrl(ProductImageService.DEFAULT_IMAGE_URL);
                        }
                    }

                    return Section.of(
                        accessoryComponent,
                        TextDisplay.of("**%s** [link](%s)".formatted(StringUtils.maxLength(product.getName(), 150), amiAmiApiService.createAmiAmiProductDetailUrl(product.getCode()))),
                        TextDisplay.of("├  Added on <t:%d:D>\n├  Current price of **%s %s**\n├  Current state **%s**\n└  `%s`".formatted(entry.getCreatedAt().toEpochSecond(), product.getPriceJpy(), Currency.JPY.getSymbol(), product.getProductState(), product.getCode()))
                    );
                })
                .toList();
            components.addAll(sections);

            // Section for page info
            components.add(Separator.createDivider(Spacing.SMALL));
            components.add(TextDisplay.of("Page %d of %d — Wishlisted products: %d".formatted(
                pageIndex + 1, page.getTotalPages(), page.getTotalElements()
            )));

            // Buttons at the bottom for pagination and mode switching
            // Previous Page
            var buttons = new ArrayList<Button>();
            if (pageIndex > 0) {
                var previousPage = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                    ButtonStyle.PRIMARY, "abc", "Previous Page"
                )), event -> {
                    event.deferEdit().complete();
                    show(user, hook, wishlist, pageIndex - 1, deleteMode);
                    return Result.REMOVE;
                });
                buttons.add(previousPage);
            } else {
                buttons.add(Button.of(ButtonStyle.PRIMARY, "disabled_previous", "Previous Page").asDisabled());
            }

            // Next Page
            if (page.getTotalPages() > 1 && pageIndex < page.getTotalPages() - 1) {
                var nextPage = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                    ButtonStyle.PRIMARY, "abc", "Next Page"
                )), event -> {
                    event.deferEdit().complete();
                    show(user, hook, wishlist, pageIndex + 1, deleteMode);
                    return Result.REMOVE;
                });
                buttons.add(nextPage);
            } else {
                buttons.add(Button.of(ButtonStyle.PRIMARY, "disabled_next", "Next Page").asDisabled());
            }

            // Add Product
            var addProductButton = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                ButtonStyle.SECONDARY, "abcd", "Add Product"
            )), event -> {
                Modal.Builder modalBuilder = Modal.create("abcd", "Add Product to %s Wishlist".formatted(StringUtils.capitalize(wishlist.getName())));
                modalBuilder.addComponents(TextDisplay.of("Please, enter the product code of the product you wish to add to your wishlist."));
                modalBuilder.addComponents(Label.of("Product Code (gcode/scode)",
                    TextInput.create(MODAL_INPUT_PRODUCT_CODE, TextInputStyle.SHORT)
                        .setPlaceholder("e.g., GOODS-04700698")
                        .setRequired(true)
                        .build())
                );

                InteractableModal interactableModal = new InteractableModal(modalBuilder, modalEvent -> {
                    var productCode = ModalUtils.getString(modalEvent.getValue(MODAL_INPUT_PRODUCT_CODE), null);

                    var modalHook = modalEvent.deferReply(true).complete();

                    if (wishlistAddCommand.handleProductAdd(user, modalHook, wishlist.getName(), productCode)) {
                        show(user, hook, wishlist, pageIndex, deleteMode);
                    }
                });

                event.replyModal(modalBuilder.build()).queue(interactableModal.registerOnCompleted());
                return Result.KEEP;
            });
            buttons.add(addProductButton);

            // Edit wishlist settings
            var editWishlistButton = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                ButtonStyle.SECONDARY, "abcd", "Edit Wishlist Settings"
            )), event -> {
                var buttonHook = event.deferEdit().complete();

                wishlistEditCommand.showMainMenu(user, buttonHook, wishlist);

                return Result.REMOVE;
            });
            buttons.add(editWishlistButton);

            // Mode Switch
            if (deleteMode) {
                var showModeButton = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                    ButtonStyle.SUCCESS, "abcd", "Show Mode"
                )), event -> {
                    event.deferEdit().complete();
                    show(user, hook, wishlist, pageIndex, false);
                    return Result.REMOVE;
                });
                buttons.add(showModeButton);
            } else {
                var deleteModeButton = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                    ButtonStyle.DANGER, "abcd", "Delete Mode"
                )), event -> {
                    event.deferEdit().complete();
                    show(user, hook, wishlist, pageIndex, true);
                    return Result.REMOVE;
                });
                buttons.add(deleteModeButton);
            }

            components.add(ActionRow.of(buttons));
        }

        var container = Container.of(components);
        messageBuilder.setComponents(container);
        hook.editOriginal(messageBuilder.build()).queue(interactableMessage.registerOnCompleted());
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var user = event.getUser();
        var focusedOption = event.getFocusedOption().getName();
        var focusedValue = event.getFocusedOption().getValue();

        if (focusedOption.equals(OPTION_WISHLIST_NAME)) {
            event.replyChoices(wishlistService.suggestWishlistNames(user.getIdLong(), focusedValue)).queue();
        }
    }
}
