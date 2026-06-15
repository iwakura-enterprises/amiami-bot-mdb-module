package enterprises.iwakura.amitracker.object;

import enterprises.iwakura.amitracker.database.entity.ChannelProductListQueryEntity;
import enterprises.iwakura.cirno.StringUtils;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class ChannelProductLIstQueryChoice extends Choice {

    public ChannelProductLIstQueryChoice(ChannelProductListQueryEntity entity) {
        super(StringUtils.shortenString("%s (#%s)".formatted(
            entity.getName(), entity.getChannel().getName()
        ), OptionData.MAX_CHOICE_NAME_LENGTH), entity.getId());
    }

    public Choice toChoice() {
        return this;
    }
}
