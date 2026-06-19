package enterprises.iwakura.amitracker.listener;

import enterprises.iwakura.amitracker.database.repository.ChannelRepository;
import enterprises.iwakura.amitracker.database.repository.GuildRepository;
import enterprises.iwakura.amitracker.database.repository.UserRepository;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Slf4j
@Bean
@RequiredArgsConstructor
public class DiscordUpdateListener extends ListenerAdapter {

    private final GuildRepository guildRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        log.info("Guild {} updated name to {}", event.getGuild().getIdLong(), event.getNewName());
        guildRepository.updateGuildName(event.getGuild().getIdLong(), event.getNewName());
    }

    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        log.info("Channel {} updated name to {}", event.getChannel().getIdLong(), event.getNewValue());
        channelRepository.updateChannelName(event.getChannel().getIdLong(), event.getNewValue());
    }

    @Override
    public void onUserUpdateGlobalName(UserUpdateGlobalNameEvent event) {
        log.info("User {} updated global name to {}", event.getUser().getIdLong(), event.getNewGlobalName());
        userRepository.updateUserName(event.getUser().getIdLong(), event.getNewGlobalName());
    }
}
