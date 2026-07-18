package enterprises.iwakura.amitracker.command;

import java.util.ArrayList;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.notify.ProductNotifyCreateCommand;
import enterprises.iwakura.amitracker.command.settings.ServerSettingsCommand;
import enterprises.iwakura.amitracker.command.settings.UserSettingsCommand;
import enterprises.iwakura.amitracker.command.wishlist.WishlistAddCommand;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.jdainteractables.Interaction;
import enterprises.iwakura.jdainteractables.InteractionHandler.Result;
import enterprises.iwakura.jdainteractables.InteractionRules;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
@Slf4j
public class WizardCommand extends AmiTrackerCommand {

    private final UserService userService;
    private final GuildService guildService;

    @Bean
    private final BeanAccessor<ProductNotifyCreateCommand> productNotifyCreateCommand = new BeanAccessor<>(ProductNotifyCreateCommand.class);

    @Bean
    private final BeanAccessor<WishlistAddCommand> wishlistAddCommand = new BeanAccessor<>(WishlistAddCommand.class);

    @Bean
    private final BeanAccessor<HelpCommand> helpCommand = new BeanAccessor<>(HelpCommand.class);

    @Bean
    private final BeanAccessor<ServerSettingsCommand> serverSettingsCommand = new BeanAccessor<>(ServerSettingsCommand.class);

    @Bean
    private final BeanAccessor<UserSettingsCommand> userSettingsCommand = new BeanAccessor<>(UserSettingsCommand.class);

    public WizardCommand(ConcurrencyService concurrencyService, UserService userService, GuildService guildService) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;

        this.name = "wizard";
        this.help = "Allows you to easily create product search notifications, wishlists, etc.";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var member = event.getMember();
        var channel = event.getGuildChannel();

        var hook = event.deferReply(true).complete();

        var userEntity = userService.getOrCreateUser(user);
        var guildEntity = Optional.ofNullable(guild).map(guildService::getOrCreateGuild).orElse(null);

        showNavigationMenu(user, guild, member, channel, hook);
    }

    private void showNavigationMenu(
        User user,
        Guild guild,
        Member member,
        GuildMessageChannelUnion channel,
        InteractionHook hook
    ) {
        var interactableMessage = new InteractableMessage();
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user));

        components.add(TextDisplay.of("## Ami Tracker Wizard"));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of("This wizard message allows you to easily interact with the bot. Please, choose an action you want to take."));

        var productSearchNotificationOption = interactableMessage.addInteraction(Interaction.asSelectOption("Create product search notification", "Sends a notification when product become available, etc.", Emoji.fromUnicode("\uD83D\uDD14")), e -> {
            productNotifyCreateCommand.getBeanInstance().runCreate(
                user, guild, member, channel, e, e
            );
            return Result.REMOVE;
        });

        var serverSettingsOption = interactableMessage.addInteraction(Interaction.asSelectOption("Server settings", "Allows you to change the preferred currency sent in notifications/product info", Emoji.fromUnicode("⚙️")), e -> {
            serverSettingsCommand.getBeanInstance().showGuildSettings(
                member, e.deferEdit().complete(), guildService.getOrCreateGuild(guild)
            );
            return Result.REMOVE;
        });

        var wishlistProductOption = interactableMessage.addInteraction(Interaction.asSelectOption("Wishlist a product", "...and also will send you notifications when it becomes available, etc.", Emoji.fromUnicode("\uD83D\uDCDD")), e -> {
            wishlistAddCommand.getBeanInstance().addUsingModal(
                user, e
            );
            return Result.REMOVE;
        });

        var userSettingsOption = interactableMessage.addInteraction(Interaction.asSelectOption("User settings", "Allows you to change your preferred currency", Emoji.fromUnicode("⚙️")), e -> {
            userSettingsCommand.getBeanInstance().showUserSettings(
                guild, member, e.deferEdit().complete(), userService.getOrCreateUser(user)
            );
            return Result.REMOVE;
        });

        var helpOption = interactableMessage.addInteraction(Interaction.asSelectOption("Help", "Shows you helpful tips for using the bot", Emoji.fromUnicode("\uD83C\uDD98")), e -> {
            helpCommand.getBeanInstance().sendHelp(e);
            return Result.KEEP;
        });

        var actionSelectMenuBuilder = StringSelectMenu.create("abc")
            .setPlaceholder("Action...");
        if (guild != null) {
            if (member.hasPermission(Permission.MANAGE_CHANNEL)) {
                actionSelectMenuBuilder.addOptions(productSearchNotificationOption);
                actionSelectMenuBuilder.addOptions(serverSettingsOption);
            }
        }
        actionSelectMenuBuilder.addOptions(wishlistProductOption, userSettingsOption, helpOption);

        components.add(ActionRow.of(actionSelectMenuBuilder.build()));

        var container = Container.of(components);
        messageBuilder.setComponents(container);
        hook.editOriginal(messageBuilder.build()).queue(interactableMessage.registerOnCompleted());
    }
}
