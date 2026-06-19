package enterprises.iwakura.amitracker.command.inventory;

import java.util.List;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.InventoryService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class InventoryRemoveCommand extends InventorySubCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";

    private final InventoryService inventoryService;
    private final UserService userService;
    private final GuildService guildService;

    public InventoryRemoveCommand(ConcurrencyService concurrencyService, InventoryService inventoryService,
        UserService userService,
        GuildService guildService
    ) {
        super(concurrencyService);
        this.inventoryService = inventoryService;
        this.userService = userService;
        this.guildService = guildService;
        this.name = "remove";
        this.help = "Remove product from your wishlist";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true, true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);

        if (productCode == null) {
            event.reply("Product code is required").setEphemeral(true).queue();
            return;
        }

        userService.getOrCreateUser(user);
        if (guild != null) {
            guildService.getOrCreateGuild(guild);
        }

        productCode = URLHelper.extractProductCode(productCode, true);

        boolean removed = inventoryService.removeProductFromInventory(user.getIdLong(), productCode);

        if (removed) {
            event.reply("Product `%s` has been removed from your inventory.".formatted(productCode)).queue();
        } else {
            event.reply("Product `%s` was not found in your inventory.".formatted(productCode)).setEphemeral(true).queue();
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var user = event.getUser();
        String focusedOption = event.getFocusedOption().getName();

        if (focusedOption.equals(OPTION_PRODUCT_CODE)) {
            event.replyChoices(inventoryService.suggestBoughtProducts(
                user.getIdLong(), event.getFocusedOption().getValue()
            )).queue();
        }
    }
}
