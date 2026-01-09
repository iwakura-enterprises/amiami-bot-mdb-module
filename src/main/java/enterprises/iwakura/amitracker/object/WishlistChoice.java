package enterprises.iwakura.amitracker.object;

import enterprises.iwakura.amitracker.database.entity.WishlistEntity;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class WishlistChoice extends Choice {

    public WishlistChoice(WishlistEntity wishlist) {
        super("%s (%d products)".formatted(wishlist.getName(), wishlist.getEntries().size()), wishlist.getName());
    }

    public Choice toChoice() {
        return this;
    }
}
