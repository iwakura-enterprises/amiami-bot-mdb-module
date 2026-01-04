package enterprises.iwakura.amitracker.command;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;

import enterprises.iwakura.amitracker.AmiTracker;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AmiTrackerCommand extends SlashCommand {

    protected final ConcurrencyService concurrencyService;
    protected AmiTracker amiTracker;

    /**
     * Initializes the command
     * @param amiTracker the AmiTracker instance
     */
    public void init(AmiTracker amiTracker) {
        this.amiTracker = amiTracker;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        concurrencyService.scheduleCommand(() -> {
            try {
                executeAsync(event);
            } catch (Exception exception) {
                log.error("Error executing command '{}'", this.name, exception);
            }
        });
    }

    protected abstract void executeAsync(SlashCommandEvent event);
}
