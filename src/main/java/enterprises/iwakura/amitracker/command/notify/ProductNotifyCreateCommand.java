package enterprises.iwakura.amitracker.command.notify;

import java.util.List;
import java.util.Objects;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
import enterprises.iwakura.amitracker.object.ProductSearchParameters;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.ProductListService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Bean
public class ProductNotifyCreateCommand extends ProductNotifySubCommand {

    public static final String OPTION_NAME = "name";
    public static final String OPTION_SEARCH_URL = "search-url";

    private final UserService userService;
    private final GuildService guildService;
    private final ProductListService productListService;

    @Bean
    private final BeanAccessor<ProductNotifyEditCommand> productNotifyEditCommand = new BeanAccessor<>(ProductNotifyEditCommand.class);

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
            new OptionData(OptionType.STRING, OPTION_NAME, "Name for the notification", true).setMaxLength(100),
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

        if (name.length() > 100) {
            name = name.substring(0, 100);
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

        var existingProductLists = productListService.getChannelProductListsByChannelId(channel.getIdLong());
        var existsSameProductQuery = existingProductLists.stream()
            .filter(channelEntity -> Objects.equals(productSearchParameters, channelEntity.getProductListQuery().getProductSearchParameters()))
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

        productNotifyEditCommand.getBeanInstance().showSettingsMenu(
            user, channel, hook, entity, productSearchParameters, true
        );
    }
}
