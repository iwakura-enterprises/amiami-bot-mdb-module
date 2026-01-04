package enterprises.iwakura.amitracker.command.inventory;

import java.util.List;
import java.util.stream.Stream;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.command.wishlist.WishlistSubCommand;
import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class InventoryOpenCommand extends InventorySubCommand {

    public InventoryOpenCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.name = "open";
        this.help = "Open your inventory";
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
    }
}
