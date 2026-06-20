package enterprises.iwakura.amitracker.command.settings;

import java.util.ArrayList;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.database.repository.GuildRepository;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.jdainteractables.Interaction;
import enterprises.iwakura.jdainteractables.InteractionHandler.Result;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
public class ServerSettingsCommand extends AmiTrackerCommand {

    private final UserService userService;
    private final GuildService guildService;
    private final GuildRepository guildRepository;

    public ServerSettingsCommand(
        ConcurrencyService concurrencyService,
        UserService userService,
        GuildService guildService, GuildRepository guildRepository
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.guildRepository = guildRepository;

        this.name = "server-settings";
        this.help = "Allows you to customize server-wide settings";
        this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var member = event.getMember();

        if (member == null || guild == null) {
            event.reply("This command is only for servers.").setEphemeral(true).queue();
            return;
        }

        if (!member.hasPermission(Permission.MANAGE_CHANNEL)) {
            event.reply("Only members with Manage Channels can change server-wide settings.").setEphemeral(true)
                .queue();
            return;
        }

        var hook = event.deferReply(true).complete();

        userService.getOrCreateUser(user);
        var guildEntity = guildService.getOrCreateGuild(guild);

        showGuildSettings(member, hook, guildEntity);
    }

    public void showGuildSettings(
        Member member,
        InteractionHook hook,
        GuildEntity guildEntity
    ) {
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var interactableMessage = new InteractableMessage();

        var preferredCurrencySelectMenuBuilder = StringSelectMenu.create("abc")
            .setPlaceholder("Currency...")
            .setMinValues(1)
            .setMaxValues(1)
            .addOptions(Currency.SELECT_OPTIONS);
        Optional.ofNullable(guildEntity.getPreferredCurrency()).map(Enum::name)
            .ifPresent(preferredCurrencySelectMenuBuilder::setDefaultValues);

        var preferredCurrencySelectMenu = interactableMessage.addInteraction(Interaction.asStringSelectMenu(preferredCurrencySelectMenuBuilder), e -> {
            if (!member.hasPermission(Permission.MANAGE_CHANNEL)) {
                e.reply("You don't hve manage Manage Channel permission!").setEphemeral(true).queue();
                hook.deleteOriginal().queue();
                return Result.REMOVE;
            }

            var menuHook = e.deferEdit().complete();
            var currency = Currency.valueOf(e.getValues().getFirst());
            guildEntity.setPreferredCurrency(currency);
            showGuildSettings(member, menuHook, guildRepository.save(guildEntity));
            return Result.REMOVE;
        });

        var secondaryCurrencySelectMenuBuilder = StringSelectMenu.create("abc")
            .setPlaceholder("Currency...")
            .setMinValues(0)
            .setMaxValues(Currency.ALL.size() - 1) // minus the primary
            .addOptions(Currency.SELECT_OPTIONS)
            .setDefaultValues(guildEntity.getSecondaryCurrencies().stream().map(Currency::name).toList());
        var secondaryCurrencySelectMenu = interactableMessage.addInteraction(Interaction.asStringSelectMenu(secondaryCurrencySelectMenuBuilder), e -> {
            if (!member.hasPermission(Permission.MANAGE_CHANNEL)) {
                e.reply("You don't hve manage Manage Channel permission!").setEphemeral(true).queue();
                hook.deleteOriginal().queue();
                return Result.REMOVE;
            }

            var menuHook = e.deferEdit().complete();
            var currencies = e.getValues().stream().map(Currency::valueOf).toList();
            guildEntity.getSecondaryCurrencies().clear();
            guildEntity.getSecondaryCurrencies().addAll(currencies);
            showGuildSettings(member, menuHook, guildRepository.save(guildEntity));
            return Result.REMOVE;
        });

        var components = new ArrayList<ContainerChildComponent>();
        components.add(TextDisplay.of("## Server Settings"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("### Preferred currency\nThe selected currency will be used in notifications, defaults for users and in other various places. Users may override this currency, but Ami Tracker will always show server's preferred currency in paratheses."));
        components.add(ActionRow.of(preferredCurrencySelectMenu));
        components.add(TextDisplay.of("### Secondary currencies\nAdditionally, you may add secondary currencies. These will be shown next to the preferred currency."));
        components.add(ActionRow.of(secondaryCurrencySelectMenu));

        var container = Container.of(components);
        messageBuilder.setComponents(container);
        hook.editOriginal(messageBuilder.build()).queue(interactableMessage.registerOnCompleted());
    }
}
