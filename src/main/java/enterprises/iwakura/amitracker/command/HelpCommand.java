package enterprises.iwakura.amitracker.command;

import java.util.ArrayList;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

@Bean
public class HelpCommand extends AmiTrackerCommand {

    public HelpCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);

        this.name = "help";
        this.help = "Shows tutorial tips how to use the bot and has FAQ";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        sendHelp(event);
    }

    public void sendHelp(IReplyCallback event) {
        var messageBuilder = new MessageCreateBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();

        components.add(TextDisplay.of("### How to get notified when a product becomes available?"));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of(
            """
            There are two ways to get notified about product state changes:
            1. Wishlist the product. See the `/wishlist add` command or `/wizard` command.
            2. Create a product search notification on a Discord server.
            The former is more flexible, as it allows you to be notified about any kind of product that falls into a specific search criteria (e.g., Touhou Fumos).
            """
        ));

        components.add(TextDisplay.of("### How to create product search notification?"));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of(
            """
            See the `/search-notification create` command or `/wizard` command.
            It boils down to these steps:
            1. Go on [amiami](https://amiami.com) and search for a product. The more specific search query, the better. You will get the best experience when you search for specific Product Lines (available from product's *About this item* section.)
            2. Copy the URL from browser's URL bar.
            3. Use the `/search-notification create` command.
            4. Inside the modal, choose a name for the product search notification and paste the copied URL there as well.
            5. Once you confirm the modal, you will be able to configure what kind of product states you want to be notified for. You can be also notified to price drops. Keep in mind that Ami Tracker always takes the lowest price for multiple pre-owned listings.
            6. Confirm the settings. Now Ami Tracker will periodically search this URL on your behalf and notify you if any new product appears and/or any product changes its state.
            Keep in mind that you require **Manage Channels** permission in order to create product search notifications on a Discord server.
            """
        ));

        components.add(TextDisplay.of("### How to change the currency to USD/EUR?"));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of(
            """
            For server-wide change, please use the `/server-settings` command. For user-specific settings, please use the `/settings` command.
            """
        ));

        components.add(TextDisplay.of("### Where can I get support / suggest new features?"));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of("Please, use the `/about` command or join the [support Discord server](https://discord.gg/3NnQ5Dpbux)."));

        components.add(TextDisplay.of("### I have reached the maximum amount of product search notifications/wishlists. How can I increase these limits?"));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of("For now, please, visit the [support Discord server](https://discord.gg/3NnQ5Dpbux) and ask there. You will get your limits increased free of charge."));

        var container = Container.of(components);
        messageBuilder.setComponents(container);
        event.reply(messageBuilder.build()).setEphemeral(true).queue();
    }
}
