package enterprises.iwakura.amitracker.command.settings;

import java.util.ArrayList;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.database.entity.UserEntity;
import enterprises.iwakura.amitracker.database.repository.UserRepository;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.jdainteractables.Interaction;
import enterprises.iwakura.jdainteractables.InteractionHandler.Result;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
public class UserSettingsCommand extends AmiTrackerCommand {

    private final UserService userService;
    private final GuildService guildService;
    private final UserRepository userRepository;

    @Bean
    private final BeanAccessor<ServerSettingsCommand> serverSettingsCommand = new BeanAccessor<>(ServerSettingsCommand.class);

    public UserSettingsCommand(ConcurrencyService concurrencyService, UserService userService,
        GuildService guildService,
        UserRepository userRepository
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.userRepository = userRepository;
        this.name = "settings";
        this.help = "Allows you to customize your settings";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var member = event.getMember();
        var hook = event.deferReply(true).complete();

        var userEntity = userService.getOrCreateUser(user);

        showUserSettings(guild, member, hook, userEntity);
    }

    public void showUserSettings(
        Guild guild,
        Member member,
        InteractionHook hook,
        UserEntity userEntity
    ) {
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var interactableMessage = new InteractableMessage();

        var currencySelectMenuBuilder = StringSelectMenu.create("abc")
            .setPlaceholder("Currency...")
            .setMinValues(1)
            .setMaxValues(1)
            .addOptions(Currency.SELECT_OPTIONS);
        Optional.ofNullable(userEntity.getPreferredCurrency()).map(Enum::name)
            .ifPresent(currencySelectMenuBuilder::setDefaultValues);

        var currencySelectMenu = interactableMessage.addInteraction(Interaction.asStringSelectMenu(currencySelectMenuBuilder), (e) -> {
            var menuHook = e.deferEdit().complete();
            var currency = Currency.valueOf(e.getValues().getFirst());
            userEntity.setPreferredCurrency(currency);
            showUserSettings(guild, member, menuHook, userRepository.save(userEntity));
            return Result.REMOVE;
        });

        var components = new ArrayList<ContainerChildComponent>();
        components.add(TextDisplay.of("## User Settings"));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of("### Preferred currency\nThe selected currency will be shown in your `/product` commands and in other various places."));
        components.add(ActionRow.of(currencySelectMenu));

        if (member != null && member.hasPermission(Permission.MANAGE_CHANNEL)) {
            components.add(Separator.createDivider(Separator.Spacing.SMALL));

            var serverSettingsButton = interactableMessage.addInteraction(Interaction.asButton(ButtonStyle.SECONDARY, "Server Settings"), e -> {
                var buttonHook = e.deferEdit().complete();
                serverSettingsCommand.getBeanInstance().showGuildSettings(
                    member,
                    buttonHook,
                    guildService.getOrCreateGuild(guild));
                return Result.REMOVE;
            });

            components.add(Section.of(
                serverSettingsButton,
                TextDisplay.of("Looking for server settings?")
            ));
        }

        var container = Container.of(components);
        messageBuilder.setComponents(container);
        hook.editOriginal(messageBuilder.build()).queue(interactableMessage.registerOnCompleted());
    }
}
