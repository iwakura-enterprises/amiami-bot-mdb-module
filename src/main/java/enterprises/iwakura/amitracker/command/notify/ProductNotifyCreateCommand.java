package enterprises.iwakura.amitracker.command.notify;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
import enterprises.iwakura.amitracker.object.ProductSearchParameters;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.GuildService;
import enterprises.iwakura.amitracker.service.LimitationService;
import enterprises.iwakura.amitracker.service.ProductListService;
import enterprises.iwakura.amitracker.service.UserService;
import enterprises.iwakura.jdainteractables.components.InteractableModal;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;

@Bean
public class ProductNotifyCreateCommand extends ProductNotifySubCommand {

    public static final String OPTION_NAME = "name";
    public static final String OPTION_SEARCH_URL = "search-url";

    private final UserService userService;
    private final GuildService guildService;
    private final ProductListService productListService;
    private final LimitationService limitationService;

    @Bean
    private final BeanAccessor<ProductNotifyEditCommand> productNotifyEditCommand = new BeanAccessor<>(ProductNotifyEditCommand.class);

    public ProductNotifyCreateCommand(ConcurrencyService concurrencyService,
        UserService userService,
        GuildService guildService, ProductListService productListService, LimitationService limitationService
    ) {
        super(concurrencyService);
        this.userService = userService;
        this.guildService = guildService;
        this.productListService = productListService;
        this.limitationService = limitationService;

        this.name = "create";
        this.help = "Creates new product search notification in this channel";

        this.userPermissions = new Permission[]{
            Permission.MANAGE_CHANNEL
        };
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var member = event.getMember();
        var channel = event.getGuildChannel();

        runCreate(user, guild, member, channel, event, event);
    }

    public void runCreate(
        User user,
        Guild guild,
        Member member,
        GuildMessageChannelUnion channel,
        IReplyCallback event,
        IModalCallback modalCallback
    ) {
        if (guild == null || member == null || channel == null) {
            event.reply("This is guild only command!").setEphemeral(true).queue();
            return;
        }

        if (!member.hasPermission(Permission.MANAGE_CHANNEL)) {
            event.reply("Only members with Manage Channels can create product search notifications.")
                .setEphemeral(true)
                .queue();
            return;
        }

        if (!channel.canTalk()) {
            event.reply("Can't send messages into this channel! Sending product notifications would not be possible.").setEphemeral(true).queue();
            return;
        }

        var modal = Modal.create("abc", "Product search notification")
            .addComponents(Label.of("Name for the notifications",
                TextInput.create(OPTION_NAME, TextInputStyle.SHORT)
                    .setPlaceholder("e.g., Touhou Fumo")
                    .setRequired(true)
                    .build()
            ))
            .addComponents(TextDisplay.of(
                """
                **TIP**: Go on amiami.com, search for products you want to get notified about and copy the URL in the URL bar in your browser. Then, paste it here.
                The Ami Tracker bot will periodically search this URL for any new products and/or product changes.
                You will get the best experience if you search for specific Product Lines / Series Title (available from product's About this item section).
                """
            ))
            .addComponents(Label.of("Search URL",
                TextInput.create(OPTION_SEARCH_URL, TextInputStyle.PARAGRAPH)
                    .setPlaceholder("e.g., https://www.amiami.com/eng/search/list/?s_seriestitle_id=9619")
                    .setRequired(true)
                    .build()
            ))
            .addComponents(TextDisplay.of(
                "Additionally, please note, that Ami Tracker searches the URL with all availability filters."
                    + " You will be able to change what kind of product availability states you want to notify on in the next step."
            ));

        var interactableModal = new InteractableModal(modal, modalEvent -> {
            var name = Optional.ofNullable(modalEvent.getValue(OPTION_NAME)).map(ModalMapping::getAsString).orElse(null);
            var searchUrl = Optional.ofNullable(modalEvent.getValue(OPTION_SEARCH_URL)).map(ModalMapping::getAsString).orElse(null);

            if (name == null || searchUrl == null) {
                modalEvent.reply("No name or search URL provided.").setEphemeral(true).queue();
                return;
            }

            if (name.length() > 100) {
                name = name.substring(0, 100);
            }

            var productSearchParameters = ProductSearchParameters.parseFromUrl(searchUrl);

            if (productSearchParameters.isEmpty()) {
                modalEvent.reply("Entered search URL does not specify any search parameters.").setEphemeral(true).queue();
                return;
            }

            var hook = modalEvent.deferReply(true).complete();

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

            var limitations = limitationService.findEffectiveLimitationForGuild(guild.getIdLong());
            var numberOfGuildChannelProductLists = productListService.countGuildChannelProductLists(guild.getIdLong());

            if (numberOfGuildChannelProductLists >= limitations.getMaxChannelProductListQueries()) {
                hook.editOriginal("You have reached the maximum number of product search notifications in this server! (%d)".formatted(
                    limitations.getMaxChannelProductListQueries()
                )).queue();
                return;
            }

            var entity = new ChannelProductListQueryEntity();
            entity.setName(name);

            productNotifyEditCommand.getBeanInstance().showSettingsMenu(
                user, channel, hook, entity, productSearchParameters, true
            );
        });

        // TODO:
        //  - Dodělat wizard příkaz (wishlist add, taky předělat na modal)
        //  - Udělat help příkaz, který bude obsahovat co bot umí, atd.

        modalCallback.replyModal(modal.build()).queue(interactableModal.registerOnCompleted());
    }

    public void showModal(
        User user,
        Guild guild,
        IModalCallback modalCallback
    ) {

    }
}
