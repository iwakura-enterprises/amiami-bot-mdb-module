package enterprises.iwakura.amitracker.command.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import net.dv8tion.jda.api.EmbedBuilder;
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
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu.Builder;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
public class ProductNotifyEditCommand extends ProductNotifySubCommand {

    public static final String OPTION_PRODUCT_SEARCH_NOTIFICATION_NAME = "search-notification-name";
    public static final int MAX_PAGEABLE_OPTION_COUNT = OptionData.MAX_CHOICES - 2;

    private final UserService userService;
    private final GuildService guildService;
    private final ProductListService productListService;

    public ProductNotifyEditCommand(
        ConcurrencyService concurrencyService, UserService userService, GuildService guildService,
        ProductListService productListService
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.productListService = productListService;
        this.name = "edit";
        this.help = "Edit product search notification settings in current server";

        this.options = List.of(
            new OptionData(OptionType.INTEGER, OPTION_PRODUCT_SEARCH_NOTIFICATION_NAME, "Exact search notification", false, true)
        );

        this.userPermissions = new Permission[]{
            Permission.MANAGE_CHANNEL
        };
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var guild = event.getGuild();

        if (guild != null) {
            var focusedOption = event.getFocusedOption().getName();
            var focusedValue = event.getFocusedOption().getValue();
            if (focusedOption.equals(OPTION_PRODUCT_SEARCH_NOTIFICATION_NAME)) {
                event.replyChoices(productListService.suggestChannelProductListQueries(guild.getIdLong(), focusedValue)).queue();
            }
        }
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var channel = event.getGuildChannel();
        var channelProductListQueryId = event.getOption(OPTION_PRODUCT_SEARCH_NOTIFICATION_NAME, OptionMapping::getAsLong);;

        if (guild == null) {
            event.reply("This is guild only command!").setEphemeral(true).queue();
            return;
        }

        var hook = event.deferReply(true).complete();

        userService.getOrCreateUser(user);
        guildService.getOrCreateGuild(guild);

        if (channelProductListQueryId == null) {
            var entities = productListService.getChannelProductListsByGuildId(guild.getIdLong());

            if (entities.isEmpty()) {
                hook.editOriginal("There are no search notifications on this server!").queue();
            } else {
                showSelectionMenu(
                    user, channel, hook, entities, 0
                );
            }
        } else {
            var optionalEntity = productListService.getChannelProductList(guild.getIdLong(), channelProductListQueryId);

            if (optionalEntity.isPresent()) {
                var entity = optionalEntity.get();
                showSettingsMenu(
                    user, channel, hook, entity, entity.getProductListQuery().getProductSearchParameters(), false
                );
            } else {
                hook.editOriginal("Could not find search notification with ID %d".formatted(channelProductListQueryId)).queue();
            }
        }
    }

    /**
     * Show selectio menu for selecting a channel product list query entity
     *
     * @param user      User
     * @param channel   Channel
     * @param hook      Hook
     * @param entities  Entities
     * @param pageIndex Page index
     */
    private void showSelectionMenu(
        User user,
        GuildChannel channel,
        InteractionHook hook,
        List<ChannelProductListQueryEntity> entities,
        int pageIndex
    ) {
        var uniqueChannelIds = entities.stream()
            .map(it -> it.getChannel().getId())
            .distinct()
            .count();
        boolean allowPagination = false;
        List<ChannelProductListQueryEntity> entitiesForPage;

        if (entities.size() > OptionData.MAX_CHOICES) {
            allowPagination = true;
            entitiesForPage = entities.subList(
                Math.clamp((long) MAX_PAGEABLE_OPTION_COUNT * pageIndex, 0, entities.size() - 1),
                Math.min(MAX_PAGEABLE_OPTION_COUNT * (pageIndex + 1), entities.size())
            );
        } else {
            entitiesForPage = entities;
        }

        var interactableMessage = new InteractableMessage();
        var messageBuilder = new MessageEditBuilder();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user));

        var embed = new EmbedBuilder();
        var description = new StringBuilder();
        embed.setTitle("Search Notifications");
        description.append("This server has %d search notifications in %d channels".formatted(
            entities.size(), uniqueChannelIds
        ));
        description.append("\n\nPlease, select a search notification from the list below.");
        if (allowPagination) {
            description.append("\nPage **%d** out of **%d**".formatted(
                pageIndex + 1, (int)Math.ceil((double) entities.size() / MAX_PAGEABLE_OPTION_COUNT)
            ));
        }
        embed.setDescription(description);

        var selectMenu = StringSelectMenu.create("abc");

        for (ChannelProductListQueryEntity entity : entitiesForPage) {
            var option = interactableMessage.addInteraction(Interaction.asSelectOption(entity.getName(), "#" + entity.getChannel().getName()), event -> {
                hook.deleteOriginal().complete();
                var optionHook = event.deferReply(true).complete();

                showSettingsMenu(user, channel, optionHook, entity, null, false);
                return Result.REMOVE;
            });
            selectMenu.addOptions(option);
        }

        if (allowPagination) {
            var previousPageOption = interactableMessage.addInteraction(Interaction.asSelectOption("Previous page", Emoji.fromUnicode("⬅️")), event -> {
                var optionHook = event.deferEdit().complete();
                showSelectionMenu(
                    user, channel, optionHook, entities, pageIndex + 1
                );
                return Result.REMOVE;
            });

            var nextPageOption = interactableMessage.addInteraction(Interaction.asSelectOption("Next page", Emoji.fromUnicode("➡️")), event -> {
                var optionHook = event.deferEdit().complete();
                showSelectionMenu(
                    user, channel, optionHook, entities, pageIndex + 1
                );
                return Result.REMOVE;
            });

            selectMenu.addOptions(previousPageOption);
            selectMenu.addOptions(nextPageOption);
        }

        messageBuilder.setComponents(ActionRow.of(selectMenu.build()));
        messageBuilder.setEmbeds(embed.build());
        hook.editOriginal(messageBuilder.build()).queue(interactableMessage.registerOnCompleted());
    }

    /**
     * Shows settings for specific entity. If create is set to true, allows user to create channel product list query
     * with the specified search parameters. If the channel product list query exists, the search parameters are not
     * editable.
     *
     * @param user                    User
     * @param channel                 Channel
     * @param hook                    Hook
     * @param entity                  Entity
     * @param productSearchParameters Product search parameters to use
     * @param create                  If user is creating new channel product list
     */
    public void showSettingsMenu(
        User user,
        GuildChannel channel,
        InteractionHook hook,
        ChannelProductListQueryEntity entity,
        final ProductSearchParameters productSearchParameters, // WARNING: Must NOT be editable!
        boolean create
    ) {
        var interactableMessage = new InteractableMessage();
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user));

        components.add(TextDisplay.of("### Product search notification - %s".formatted(StringUtils.capitalizeAllWords(entity.getName()))));
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

        var selectedRoles = entity.getRoleIdsToNotify().stream()
            .map(DefaultValue::role)
            .limit(SelectMenu.OPTIONS_MAX_AMOUNT)
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
        components.add(TextDisplay.of("```\n%s\n```".formatted(
            Optional.ofNullable(productSearchParameters)
                .orElseGet(() -> entity.getProductListQuery().getProductSearchParameters())
                .toDiscordMessage()
        )));

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
                    channel, entity, productSearchParameters // Shouldn't be null
                );

                if (errorContext.isSuccess()) {
                    buttonHook.editOriginal("Product search notification '%s' created successfully!".formatted(entity.getName())).queue();
                } else {
                    switch (errorContext.getType()) {
                        case SEARCH_PARAMETERS_EMPTY -> buttonHook.editOriginal("No search parameters!").queue();
                        case CHANNEL_PRODUCT_LIST_DUPLICATE_NAME -> buttonHook.editOriginal("Search notification with this name already exists in this channel!").queue();
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
