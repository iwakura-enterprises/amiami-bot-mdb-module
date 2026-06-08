package enterprises.iwakura.amitracker.config;

import lombok.Data;

@Data
public class ProductChangeAnnouncementConfiguration {

    private long recentRolePingBackoff = 2 * 60 * 1000; // 2 minutes
}
