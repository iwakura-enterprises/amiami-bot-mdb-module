package enterprises.iwakura.amitracker.command.inventory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.InventoryService;
import enterprises.iwakura.amitracker.service.ProductImageService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.util.ModalUtils;
import enterprises.iwakura.amitracker.util.NumberUtils;
import enterprises.iwakura.amitracker.util.StringUtils;
import enterprises.iwakura.jdainteractables.Interaction;
import enterprises.iwakura.jdainteractables.InteractionHandler.Result;
import enterprises.iwakura.jdainteractables.InteractionRules;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.jdainteractables.components.InteractableModal;
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
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
public class InventoryOpenCommand extends InventorySubCommand {

    public static final String OPTION_DELETE_MODE = "delete-mode";
    public static final String MODAL_INPUT_PRODUCT_CODE = "product-code";
    public static final String MODAL_INPUT_PURCHASE_DATE = "purchase-date";
    public static final String MODAL_INPUT_PRICE = "price";
    public static final String MODAL_INPUT_CURRENCY = "currency";
    public static final int PAGE_SIZE = 5;

    private final InventoryService inventoryService;
    private final UserService userService;
    private final GuildService guildService;
    private final ProductImageService productImageService;
    private final AmiAmiApiService amiAmiApiService;

    private final InventoryAddCommand inventoryAddCommand;

    public InventoryOpenCommand(
        ConcurrencyService concurrencyService, InventoryService inventoryService,
        UserService userService,
        GuildService guildService, ProductImageService productImageService, AmiAmiApiService amiAmiApiService,
        InventoryAddCommand inventoryAddCommand
    ) {
        super(concurrencyService);
        this.inventoryService = inventoryService;
        this.userService = userService;
        this.guildService = guildService;
        this.productImageService = productImageService;
        this.amiAmiApiService = amiAmiApiService;
        this.inventoryAddCommand = inventoryAddCommand;
        this.name = "open";
        this.help = "Open your inventory";

        this.options = List.of(
            new OptionData(OptionType.BOOLEAN, OPTION_DELETE_MODE, "Enable delete mode", false)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var deleteMode = event.getOption(OPTION_DELETE_MODE, false, OptionMapping::getAsBoolean);
        var user = event.getUser();
        var guild = event.getGuild();

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        var hook = event.deferReply().complete();
        show(user, hook, 0, deleteMode);
    }

    private void show(User user, InteractionHook hook, int pageIndex, boolean deleteMode) {
        var interactableMessage = new InteractableMessage();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user)); // Only allow the command user to interact
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();
        var buttons = new ArrayList<Button>();
        components.add(TextDisplay.of("### %s's Inventory".formatted(user.getAsMention())));
        components.add(Separator.createDivider(Spacing.SMALL));

        var page = inventoryService.getInventoryPage(user.getIdLong(), PAGE_SIZE, pageIndex);


        // Add Product
        var addProductButton = interactableMessage.addInteraction(Interaction.asButton(Button.of(
            ButtonStyle.SECONDARY, "abcd", "Add Product"
        )), event -> {
            Modal.Builder modalBuilder = Modal.create("abcd", "Add Product to your Inventory");
            modalBuilder.addComponents(TextDisplay.of("Please, enter the product code of the product you wish to add to your inventory."));
            modalBuilder.addComponents(Label.of("Product Code (gcode/scode)",
                TextInput.create(MODAL_INPUT_PRODUCT_CODE, TextInputStyle.SHORT)
                    .setPlaceholder("e.g., GOODS-04700698")
                    .setRequired(true)
                    .build())
            );
            modalBuilder.addComponents(Label.of("Date of Purchase (YYYY-MM-DD)",
                TextInput.create(MODAL_INPUT_PURCHASE_DATE, TextInputStyle.SHORT)
                    .setPlaceholder("e.g., 2026-12-31 (defaults to today)")
                    .setRequired(false)
                    .setMaxLength(10)
                    .setMinLength(10)
                    .build())
            );
            modalBuilder.addComponents(Label.of("Bought at Price",
                TextInput.create(MODAL_INPUT_PRICE, TextInputStyle.SHORT)
                    .setPlaceholder("e.g., 17600 (defaults to product's current price)")
                    .setRequired(false)
                    .build())
            );
            modalBuilder.addComponents(Label.of("Currency",
                TextInput.create(MODAL_INPUT_CURRENCY, TextInputStyle.SHORT)
                    .setPlaceholder("e.g., JPY (defaults to JPY; other: %s)".formatted(Currency.CHOICES_STRING))
                    .setRequired(false)
                    .setMaxLength(3)
                    .build())
            );

            InteractableModal interactableModal = new InteractableModal(modalBuilder, modalEvent -> {
                var productCode = ModalUtils.getString(modalEvent.getValue(MODAL_INPUT_PRODUCT_CODE), null);
                var purchaseDateString = ModalUtils.getString(modalEvent.getValue(MODAL_INPUT_PURCHASE_DATE), null);
                var priceDouble = Optional.ofNullable(ModalUtils.getString(modalEvent.getValue(MODAL_INPUT_PRICE), null))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(NumberUtils::parseSafe)
                    .orElse(null);
                var currencyString = ModalUtils.getString(modalEvent.getValue(MODAL_INPUT_CURRENCY), Currency.JPY.name());

                var now = OffsetDateTime.now();
                var purchaseDate = purchaseDateString == null ? now : OffsetDateTime.parse(purchaseDateString + "T00:00:00+00:00");
                var currency = Currency.fromString(currencyString).orElse(null);

                var modalHook = modalEvent.deferReply(true).complete();

                var success = inventoryAddCommand.handleProductAdd(user, modalHook, productCode, purchaseDate, priceDouble, currency);
                if (success) {
                    // If user added a product for today, show first page to see it
                    int preferredPageIndex = purchaseDate == now ? 0 : pageIndex;
                    show(user, hook, preferredPageIndex, deleteMode);
                }
            });

            event.replyModal(modalBuilder.build()).queue(interactableModal.registerOnCompleted());
            return Result.KEEP;
        });
        buttons.add(addProductButton);

        if (page.getTotalElements() == 0) {
            components.add(TextDisplay.of("Your inventory is empty."));
        } else {
            // Main sections for each bought product
            var sections = page.getContent()
                .stream()
                .map(boughtProduct -> {
                    var product = boughtProduct.getProduct();
                    SectionAccessoryComponent accessoryComponent;

                    if (deleteMode) {
                        accessoryComponent = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                            ButtonStyle.DANGER, "abcd", "Delete Product"
                        )), event -> {
                            event.deferEdit().complete();
                            inventoryService.removeProductFromInventory(user.getIdLong(), product.getCode());

                            // If user deleted the last item on the page, go back one page
                            if (page.getContent().size() == 1) {
                                show(user, hook, Math.max(0, pageIndex - 1), true);
                            } else {
                                show(user, hook, pageIndex, true);
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
                        TextDisplay.of("├  Bought on <t:%d:D>\n├  For price of **%s %s**\n└  `%s`".formatted(boughtProduct.getBoughtAt().toEpochSecond(), boughtProduct.getPriceJpy(), Currency.JPY.getSymbol(), product.getCode()))
                    );
                })
                .toList();
            components.addAll(sections);

            // Section for page info
            components.add(Separator.createDivider(Spacing.SMALL));
            components.add(TextDisplay.of("Page %d of %d — Bought products: %d".formatted(
                pageIndex + 1, page.getTotalPages(), page.getTotalElements()
            )));

            // Buttons at the bottom for pagination and mode switching
            // Previous Page
            if (pageIndex > 0) {
                var previousPage = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                    ButtonStyle.PRIMARY, "abc", "Previous Page"
                )), event -> {
                    event.deferEdit().complete();
                    show(user, hook, pageIndex - 1, deleteMode);
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
                    show(user, hook, pageIndex + 1, deleteMode);
                    return Result.REMOVE;
                });
                buttons.add(nextPage);
            } else {
                buttons.add(Button.of(ButtonStyle.PRIMARY, "disabled_next", "Next Page").asDisabled());
            }

            // Mode Switch
            if (deleteMode) {
                var showModeButton = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                    ButtonStyle.SUCCESS, "abcd", "Show Mode"
                )), event -> {
                    event.deferEdit().complete();
                    show(user, hook, pageIndex, false);
                    return Result.REMOVE;
                });
                buttons.add(showModeButton);
            } else {
                var deleteModeButton = interactableMessage.addInteraction(Interaction.asButton(Button.of(
                    ButtonStyle.DANGER, "abcd", "Delete Mode"
                )), event -> {
                    event.deferEdit().complete();
                    show(user, hook, pageIndex, true);
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
}
