package enterprises.iwakura.amitracker.command.notify;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;

@Bean
public class ProductNotifyCreateAdvancedCommand extends ProductNotifySubCommand {

    public static final String MODAL_INPUT_NAME = "name";
    public static final String MODAL_INPUT_SEARCH_KEYWORD = "search-keyword";

    private final UserService userService;
    private final GuildService guildService;

    public ProductNotifyCreateAdvancedCommand(ConcurrencyService concurrencyService,
        UserService userService,
        GuildService guildService
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.name = "create-advanced";
        this.help = "Create advanced product notification in current channel";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        Modal.Builder modalBuilder = Modal.create("abcd", "Create Product Notifications");
        modalBuilder.addComponents(TextDisplay.of("This menu allows you to define what products should be "
            + "searched on AmiAmi periodically and notify this channel when there are updates."));
        modalBuilder.addComponents(Label.of("Name",
            TextInput.create(MODAL_INPUT_NAME, TextInputStyle.SHORT)
                .setPlaceholder("e.g., Fumo")
                .setRequired(true)
                .setMaxLength(100)
                .setMinLength(3)
                .build()));
        modalBuilder.addComponents(TextDisplay.of("You may choose by which fields the products will be filtered. "
            + "At least one of the filters should be set."));
        modalBuilder.addComponents(Label.of("Search keyword",
            TextInput.create(MODAL_INPUT_SEARCH_KEYWORD, TextInputStyle.SHORT)
                .setPlaceholder("e.g., Touhou FumoFumo")
                .setRequired(true)
                .setMaxLength(100)
                .setMinLength(3)
                .build()));
        // TODO:
    }
}
