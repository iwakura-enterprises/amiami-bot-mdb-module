package enterprises.iwakura.amitracker.config;

import lombok.Data;

@Data
public class SentryConfiguration {

    private String dsn = "https://sentry-dsn.example.com";

}
