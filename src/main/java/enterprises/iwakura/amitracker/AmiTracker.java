package enterprises.iwakura.amitracker;

import java.util.List;

import com.jagrosh.jdautilities.command.CommandClientBuilder;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.command.SubCommand;
import enterprises.iwakura.amitracker.service.AmiAmiQueryService;
import enterprises.iwakura.amitracker.service.ConfigurationService;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.amitracker.service.scheduler.ProductQueryScheduler;
import enterprises.iwakura.modularbot.base.Module;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@RequiredArgsConstructor
public class AmiTracker extends Module {

    private final ConfigurationService configurationService;
    private final DatabaseService databaseService;
    private final AmiAmiQueryService amiAmiQueryService;

    private final ProductQueryScheduler productQueryScheduler;

    private final List<AmiTrackerCommand> discordCommands;

    @Override
    public void onEnable() {
        var info = getModuleInfo();
        log.info("# {} @ {}", info.getName(), info.getVersion());
        log.info("> Made by {}", info.getAuthor());
        log.info("Starting up...");
        var startMillis = System.currentTimeMillis();

        log.info("");
        log.info("fumo fumo~");
        log.info("");

        configurationService.init(this.getModuleDirectoryPath());
        databaseService.initialize();
        amiAmiQueryService.init();
        productQueryScheduler.initialize();

        log.info("{} started in {} ms", info.getName(), System.currentTimeMillis() - startMillis);
    }

    @Override
    public void onDisable() {
        var info = getModuleInfo();
        log.info("Stopping {} @ {}", info.getName(), info.getVersion());

        log.info("o/");
    }

    @Override
    public void onCommandClientBuilderInitialization(@NonNull CommandClientBuilder commandClientBuilder) {
        discordCommands.forEach(command -> {
            if (!(command instanceof SubCommand)) {
                log.info("Registering slash command: {}", command.getName());
                command.init(this);
                commandClientBuilder.addSlashCommand(command);
            }
        });
    }
}
