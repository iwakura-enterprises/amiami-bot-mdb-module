package enterprises.iwakura.amitracker.service;

import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.database.repository.GuildRepository;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;

@Bean
@Slf4j
@RequiredArgsConstructor
public class GuildService {

    private final GuildRepository guildRepository;

    /**
     * Get or create a guild entity based on the Discord guild.
     *
     * @param guild Discord guild
     *
     * @return The existing or newly created GuildEntity
     */
    public GuildEntity getOrCreateGuild(Guild guild) {
        return guildRepository.getOrCreate(guild.getIdLong(), guild.getName());
    }
}
