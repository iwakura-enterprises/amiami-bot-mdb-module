package enterprises.iwakura.amitracker.object;

import enterprises.iwakura.amitracker.database.entity.ProductEntity;
import enterprises.iwakura.amitracker.util.StringUtils;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

/**
 * Extends the Choice class to create a product choice with a formatted name and code.
 */
public class ProductChoice extends Choice {

    public ProductChoice(ProductEntity product) {
        super(StringUtils.maxLength("%s (%s".formatted(product.getCode(), product.getName()), OptionData.MAX_CHOICE_NAME_LENGTH) + ")", product.getCode());
    }

    public Choice toChoice() {
        return this;
    }
}
