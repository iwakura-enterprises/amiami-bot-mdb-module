package enterprises.iwakura.amitracker.command.wishlist;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.command.SubCommand;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import net.dv8tion.jda.api.interactions.InteractionContextType;

public abstract class WishlistSubCommand extends AmiTrackerCommand implements SubCommand {

    public WishlistSubCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.contexts = InteractionContextType.ALL.toArray(new InteractionContextType[0]);
    }
}
