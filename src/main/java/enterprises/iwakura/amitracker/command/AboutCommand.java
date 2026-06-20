package enterprises.iwakura.amitracker.command;

import java.util.List;
import java.util.Map;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.AmiTracker;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.modularbot.ModularBotConstants;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.DiscordLocale;

@Bean
public class AboutCommand extends AmiTrackerCommand {

    public AboutCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "about";
        this.help = "Displays information about the Ami Tracker bot.";
        this.descriptionLocalization = Map.of(
            DiscordLocale.CZECH, "Zobrazí informace o Ami Tracker botovi."
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        event.deferReply(true).complete();

        List<MessageTopLevelComponent> components = List.of(
            Container.of(
                Section.of(
                    Thumbnail.fromUrl("https://goddrinksjava.net/akasha/data-source/hetzner/public/logo/ami-tracker-symbol.png")
                        .withDescription("Logo of Ami Tracker showing a silly fumo face."),
                    TextDisplay.of("# Ami Tracker"),
                    TextDisplay.of("A companion bot for accessing, bookmarking and tracking AmiAmi™ and products there.")
                ),
                ActionRow.of(
                    Button.of(
                        ButtonStyle.LINK,
                        "https://example.com",
                        "Support",
                        Emoji.fromUnicode("🔗")
                    ),
                    Button.of(
                        ButtonStyle.LINK,
                        "https://example.com",
                        "Terms of Service",
                        Emoji.fromUnicode("🔗")
                    )
                ),
                TextDisplay.of("All products and company names sent by this bot are registered trademarks of "
                    + "their respective holders. **This bot or its services are not affiliated with or endorsed by AmiAmi™**"),
                TextDisplay.of("-# For any questions, concerns or requests, please contact the bot developer at `mayuna@iwakura.enterprises`"),
                TextDisplay.of("-# Abusing functionalities provided by this bot may result in a permanent blacklist from using its services."),
                Separator.createDivider(Separator.Spacing.SMALL),
                TextDisplay.of(("This bot runs on [Modular Discord Bot framework](https://github.com/iwakura-enterprises/modular-discord-bot) (%s) and "
                    + "is powered by [JDA](https://github.com/discord-jda/JDA)").formatted(ModularBotConstants.getVersion())),
                TextDisplay.of("Developed with ❤ by [Iwakura Enterprises](https://iwakura.enterprises)"),
                TextDisplay.of("-# Version " + amiTracker.getModuleInfo().getVersion())
            )
        );

        event.getHook().editOriginalComponents(components)
            .useComponentsV2()
            .queue();
    }
}
