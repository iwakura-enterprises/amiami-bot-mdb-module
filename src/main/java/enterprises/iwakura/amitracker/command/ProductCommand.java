package enterprises.iwakura.amitracker.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.AmiTracker;
import enterprises.iwakura.amitracker.constant.Currency;
import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.ProductService;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.cirno.StringUtils;
import enterprises.iwakura.jdainteractables.InteractionRules;
import enterprises.iwakura.jdainteractables.components.InteractableMessage;
import enterprises.iwakura.kirara.amiami.AmiAmiApi;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

@Bean
@Slf4j
public class ProductCommand extends AmiTrackerCommand {

    public static final String OPTION_PRODUCT_CODE = "product-code";

    private final ProductService productService;
    private final AmiAmiApiService amiAmiApiService;

    public ProductCommand(ConcurrencyService concurrencyService, ProductService productService,
        AmiAmiApiService amiAmiApiService
    ) {
        super(concurrencyService);
        this.productService = productService;
        this.amiAmiApiService = amiAmiApiService;

        this.name = "product";
        this.help = "Get product information";

        this.options = List.of(
            new OptionData(OptionType.STRING, OPTION_PRODUCT_CODE, "Product code (gcode/scode)", true, true)
        );
    }

    @Override
    protected void executeAsync(SlashCommandEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();
        var productCode = event.getOption(OPTION_PRODUCT_CODE, OptionMapping::getAsString);

        var hook = event.deferReply().complete();

        if (productCode == null) {
            hook.editOriginal("Product code is required").queue();
            return;
        }

        productCode = URLHelper.extractProductCode(productCode);
        log.info("User {} requesting product {}", user.getIdLong(), productCode);

        var optionalProduct = productService.getOrQueryProduct(productCode);

        if (optionalProduct.isEmpty()) {
            hook.editOriginal("Product with code `" + productCode + "` not found").queue();
            return;
        }

        var product = optionalProduct.get();
        var interactableMessage = new InteractableMessage();
        interactableMessage.addInteractionRule(InteractionRules.allowUsers(user));
        var messageBuilder = new MessageEditBuilder().useComponentsV2();
        var components = new ArrayList<ContainerChildComponent>();
        components.add(TextDisplay.of("## [%s](%s)".formatted(
            StringUtils.capitalize(product.getName()),
            amiAmiApiService.createAmiAmiProductDetailUrl(product.getCode())
        )));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of(productService.createProductInfoDescription(product)));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(TextDisplay.of("### Price history\nTODO"));
        components.add(Separator.createDivider(Spacing.SMALL));
        components.add(MediaGallery.of(
            MediaGalleryItem.fromUrl(AmiTracker.AMI_AMI_IMAGE_URL.formatted(product.getImageUrl()))
        ));
        messageBuilder.setComponents(Container.of(components));
        hook.editOriginal(messageBuilder.build()).queue(interactableMessage.registerOnCompleted());
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        var focusedOption = event.getFocusedOption().getName();
        var focusedValue = event.getFocusedOption().getValue();
        if (focusedOption.equals(OPTION_PRODUCT_CODE)) {
            event.replyChoices(productService.suggestProductCodes(focusedValue)).queue();
        }
    }
}
