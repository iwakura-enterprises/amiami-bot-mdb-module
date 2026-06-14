package enterprises.iwakura.amitracker.command.notify;

import java.util.ArrayList;
import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
import enterprises.iwakura.amitracker.object.ProductSearchParameters;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.ProductListService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.cirno.StringUtils;
import enterprises.iwakura.jdainteractables.Interaction;
import enterprises.iwakura.jdainteractables.InteractionHandler.Result;
import enterprises.iwakura.jdainteractables.InteractionRules;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.DefaultValue;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
public class ProductNotifyCreateCommand extends ProductNotifySubCommand {

    public static final String OPTION_NAME = "name";
    public static final String OPTION_SEARCH_URL = "search-url";

    private final UserService userService;
    private final GuildService guildService;
    private final ProductListService productListService;

    public ProductNotifyCreateCommand(ConcurrencyService concurrencyService,
        UserService userService,
        GuildService guildService, ProductListService productListService
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.productListService = productListService;

        this.name = "create";
        this.help = "Creates new product search notification in this channel";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_NAME, "Name for the notification", true),
            new OptionData(OptionType.STRING, OPTION_SEARCH_URL, "The URL when searching for products on AmiAmi", true)
        );

        this.userPermissions = new Permission[]{
            Permission.MANAGE_CHANNEL
        };
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var channel = event.getGuildChannel();
        var name = event.getOption(OPTION_NAME, OptionMapping::getAsString);;
        var searchUrl = event.getOption(OPTION_SEARCH_URL, OptionMapping::getAsString);

        if (guild == null) {
            event.reply("This is guild only command!").setEphemeral(true).queue();
            return;
        }

        if (name == null || searchUrl == null) {
            event.reply("No name or search URL provided.").setEphemeral(true).queue();
            return;
        }

        if (!channel.canTalk()) {
            event.reply("Can't send messages into this channel! Sending product notifications would not be possible.").setEphemeral(true).queue();
            return;
        }

        var productSearchParameters = ProductSearchParameters.parseFromUrl(searchUrl);

        if (productSearchParameters.isEmpty()) {
            event.reply("Entered search URL does not specify any search parameters.").setEphemeral(true).queue();
            return;
        }

        var hook = event.deferReply(true).complete();

        userService.getOrCreateUser(user);
        guildService.getOrCreateGuild(guild);

        var existingProductLists = productListService.getChannelProductLists(channel.getIdLong());
        var existsSameProductQuery = existingProductLists.stream()
            .filter(channelEntity -> channelEntity.getProductListQuery().getProductSearchParameters().equals(productSearchParameters))
            .map(ChannelProductListQueryEntity::getName)
            .findAny();

        if (existsSameProductQuery.isPresent()) {
            hook.editOriginal("This channel already has a product search notification with the same search parameters under the name '%s'!".formatted(
                existsSameProductQuery.get()
            )).queue();
            return;
        }

        var entity = new ChannelProductListQueryEntity();
        entity.setName(name);

        showSettingsMenu(
            user, channel, hook, entity, productSearchParameters, true
        );

        // TODO:
        //  - command managing product lists
    }

    public void showSettingsMenu(
        User user,
        GuildChannel channel,
        InteractionHook hook,
        ChannelProductListQueryEntity entity,
        ProductSearchParameters productSearchParameters,
        boolean create
    ) {
        var interactableMessage = new InteractableMessage();
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user));

        components.add(TextDisplay.of("### New product search notification %s".formatted(StringUtils.capitalize(entity.getName()))));
        components.add(Separator.createDivider(Spacing.SMALL));

        var priceDiscountButton = interactableMessage.addInteraction(Interaction.asButton(createToggleButton(entity.isPriceDiscountEnabled())), event -> {
            var discountButtonHook = event.deferEdit().complete();

            boolean newState = !entity.isPriceDiscountEnabled();
            entity.setPriceDiscountEnabled(newState);

            if (!create) {
                productListService.save(entity);
            }

            showSettingsMenu(user, channel, discountButtonHook, entity, productSearchParameters, create);

            return Result.REMOVE;
        });

        var stockChangeButton = interactableMessage.addInteraction(Interaction.asButton(createToggleButton(entity.isStockChangeEnabled())), event -> {
            var stockChangeButtonHook = event.deferEdit().complete();

            boolean newState = !entity.isStockChangeEnabled();
            entity.setStockChangeEnabled(newState);

            if (!create) {
                productListService.save(entity);
            }

            showSettingsMenu(user, channel, stockChangeButtonHook, entity, productSearchParameters, create);

            return Result.REMOVE;
        });

        // TODO: Limit to OPTIONS_MAX_AMOUNT
        var selectedRoles = entity.getRoleIdsToNotify().stream()
                .map(DefaultValue::role)
                .toList();
        var roleEntitySelectMenu = EntitySelectMenu.create("abc", SelectTarget.ROLE)
            .setDefaultValues(selectedRoles)
            .setMaxValues(SelectMenu.OPTIONS_MAX_AMOUNT)
            .setPlaceholder("Roles to ping...");
        var roleSelectMenu = interactableMessage.addInteraction(Interaction.asEntitySelectMenu(roleEntitySelectMenu), event -> {
            var selectMenuHook = event.deferEdit().complete();

            var roleIds = event.getValues().stream().map(ISnowflake::getIdLong).toList();
            entity.getRoleIdsToNotify().clear();
            entity.getRoleIdsToNotify().addAll(roleIds);

            if (!create) {
                productListService.save(entity);
            }

            showSettingsMenu(user, channel, selectMenuHook, entity, productSearchParameters, create);

            return Result.REMOVE;
        });

        components.add(TextDisplay.of("**Search Parameters**"));
        components.add(TextDisplay.of("```\n%s\n```".formatted(productSearchParameters.toDiscordMessage())));

        components.add(
            Section.of(
                priceDiscountButton,
                TextDisplay.of("**Price Discount Alerts**"),
                TextDisplay.of("Enables or disables price discount alerts for this search notification.\nWhen enabled, this channel will receive notifications **when products in the search result have price drops**.")
            )
        );

        components.add(
            Section.of(
                stockChangeButton,
                TextDisplay.of("**Stock Change Alerts**"),
                TextDisplay.of("Enables or disables stock change alerts for this search notification.\nWhen enabled, this channel will receive notifications **when products in this search result change their stock status**.")
            )
        );

        components.add(TextDisplay.of("**Roles to ping**"));
        components.add(TextDisplay.of("Whenever a notification will be sent into this channel, the selected roles will be pinged.\nRole pings will be suppressed for notifications in a short span of time to prevent ping spamming."));
        components.add(ActionRow.of(roleSelectMenu));

        // Buttons
        var buttons = new ArrayList<Button>();
        if (create) {
            var confirmButton = interactableMessage.addInteraction(Interaction.asButton(Button.primary("abc", "Confirm")), event -> {
                hook.deleteOriginal().complete();
                var buttonHook = event.deferReply(true).complete();

                var errorContext = productListService.createChannelProductListQuery(
                    channel, entity, productSearchParameters
                );

                if (errorContext.isSuccess()) {
                    buttonHook.editOriginal("Product search notification '%s' created successfully!".formatted(entity.getName())).queue();
                } else {
                    switch (errorContext.getType()) {
                        case SEARCH_PARAMETERS_EMPTY -> buttonHook.editOriginal("No search parameters!").queue();
                        default -> buttonHook.editOriginal("Unknown error.").queue();
                    }
                }

                return Result.REMOVE;
            });
            buttons.add(confirmButton);
        } else {
            var deleteButton = interactableMessage.addInteraction(Interaction.asButton(Button.danger("abc", "Delete")), event -> {
                hook.deleteOriginal().complete();
                var buttonHook = event.deferReply(true).complete();

                var errorContext = productListService.deleteChannelProductListQuery(entity.getId());

                if (errorContext.isSuccess()) {
                    buttonHook.editOriginal("Product search notification '%s' deleted successfully!".formatted(entity.getName())).queue();
                } else {
                    switch (errorContext.getType()) {
                        case CHANNEL_PRODUCT_LIST_NOT_FOUND -> buttonHook.editOriginal("Product search notification '%s' does not exist.".formatted(entity.getName())).queue();
                        default -> buttonHook.editOriginal("An unknown error occurred while deleting the product search notification.").queue();
                    }
                }

                return Result.REMOVE;
            });
            buttons.add(deleteButton);
        }

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
}
