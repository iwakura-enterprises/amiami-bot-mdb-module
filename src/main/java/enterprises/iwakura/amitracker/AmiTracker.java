package enterprises.iwakura.amitracker;

import java.util.List;

import com.jagrosh.jdautilities.command.CommandClientBuilder;

import enterprises.iwakura.amitracker.command.AmiTrackerCommand;
import enterprises.iwakura.amitracker.command.SubCommand;
import enterprises.iwakura.amitracker.service.AmiAmiQueryService;
import enterprises.iwakura.amitracker.service.ConfigurationService;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.amitracker.service.ProductChangeAnnounceService;
import enterprises.iwakura.amitracker.service.scheduler.BaseScheduler;
import enterprises.iwakura.amitracker.service.scheduler.ProductQueryScheduler;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.modularbot.base.Module;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

@Slf4j
@Bean
@RequiredArgsConstructor
public class AmiTracker extends Module {

    public static final String AMI_AMI_IMAGE_URL = "https://img.amiami.com/%s";
    public static final String IMAGE_NOT_FOUND_URL = "https://goddrinksjava.net/akasha/data-source/hetzner/public/amitracker/product/404.png";

    private final ConfigurationService configurationService;
    private final DatabaseService databaseService;
    private final AmiAmiQueryService amiAmiQueryService;
    private final ProductChangeAnnounceService productChangeAnnounceService;

    private final List<ListenerAdapter> listeners;

    private final List<AmiTrackerCommand> discordCommands;
    private final List<GanyuCommand> consoleCommands;
    private final List<BaseScheduler> baseSchedulers;

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
        productChangeAnnounceService.init();
        baseSchedulers.forEach(BaseScheduler::initialize);

        log.info("{} started in {} ms", info.getName(), System.currentTimeMillis() - startMillis);
    }

    @Override
    public void onDisable() {
        var info = getModuleInfo();
        log.info("Stopping {} @ {}", info.getName(), info.getVersion());

        log.info("o/");
    }

    @Override
    public void onShardManagerBuilderInitialization(@NonNull DefaultShardManagerBuilder shardManagerBuilder) {
        listeners.forEach(listener -> {
            log.info("Registering listener: {}", listener.getClass().getName());
            shardManagerBuilder.addEventListeners(listener);
        });

        shardManagerBuilder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        shardManagerBuilder.enableIntents(GatewayIntent.GUILD_MESSAGES);
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

    @Override
    public void onConsoleCommandRegistration(@NonNull Ganyu ganyu) {
        consoleCommands.forEach(ganyu::registerCommands);
    }
}
