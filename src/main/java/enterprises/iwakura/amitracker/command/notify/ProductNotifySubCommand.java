package enterprises.iwakura.amitracker.command.notify;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.command.SubCommand;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import net.dv8tion.jda.api.interactions.InteractionContextType;

public abstract class ProductNotifySubCommand extends AmiTrackerCommand implements SubCommand {

    public ProductNotifySubCommand(ConcurrencyService concurrencyService) {
        super(concurrencyService);
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
    }
}
