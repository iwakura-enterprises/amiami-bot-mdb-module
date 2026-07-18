package enterprises.iwakura.amitracker.service;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.log4j2.SentryAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@RequiredArgsConstructor
public class SentryService {

    private final ConfigurationService configurationService;

    /**
     * Initializes sentry
     */
    public void init() {
        log.info("Initializing Sentry...");

        var config = configurationService.getSentryConfiguration();

        if (config.getDsn() == null || config.getDsn().isBlank()) {
            log.warn("Sentry is disabled!");
            return;
        }

        var sentryOptions = new SentryOptions();
        sentryOptions.setDsn(config.getDsn());
        Sentry.init(sentryOptions);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = ctx.getConfiguration();
        Appender appender = SentryAppender.createAppender("Sentry", Level.INFO, Level.ERROR, null, false, null, null);
        appender.start();
        log4jConfig.getRootLogger().addAppender(appender, null, null);
        ctx.updateLoggers();
    }
}
