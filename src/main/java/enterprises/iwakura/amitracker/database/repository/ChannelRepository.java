package enterprises.iwakura.amitracker.database.repository;

import enterprises.iwakura.amitracker.database.entity.ChannelEntity;
import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import enterprises.iwakura.amitracker.service.DatabaseService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

@Slf4j
@Bean
public class ChannelRepository extends AmiBaseRepository<ChannelEntity, Long> {

    @Bean
    private final BeanAccessor<GuildRepository> guildRepository = new BeanAccessor<>(GuildRepository.class);

    /**
     * Initializes the repository with the database service.
     *
     * @param databaseService the database service to use
     */
    public ChannelRepository(DatabaseService databaseService) {
        super(databaseService);
    }

    @Override
    public Class<ChannelEntity> getEntityClass() {
        return ChannelEntity.class;
    }

    @Override
    protected boolean hasId(ChannelEntity channelEntity) {
        return existsById(channelEntity.getId());
    }

    /**
     * Get or create a channel by ID and name.
     *
     * @param channel Channel
     *
     * @return The existing or newly created GuildEntity
     */
    public ChannelEntity getOrCreate(GuildChannel channel) {
        final var guild = channel.getGuild();

        return databaseService.runInThreadTransaction(session -> {
            return findById(channel.getIdLong()).orElseGet(() -> {
                log.info("Creating ChannelEntity for {} (in guild {}, name {})",
                    channel.getIdLong(), guild.getIdLong(), channel.getName()
                );
                var newChannelEntity = new ChannelEntity();
                newChannelEntity.setId(channel.getIdLong());
                newChannelEntity.setName(channel.getName());
                newChannelEntity.setGuild(guildRepository.getBeanInstance().findById(guild.getIdLong()).orElseThrow());
                return save(newChannelEntity);
            });
        });
    }
}
