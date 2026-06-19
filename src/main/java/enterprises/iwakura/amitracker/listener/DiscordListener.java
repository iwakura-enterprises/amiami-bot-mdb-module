package enterprises.iwakura.amitracker.listener;

import enterprises.iwakura.amitracker.command.ProductCommand;
import enterprises.iwakura.amitracker.database.repository.ChannelRepository;
import enterprises.iwakura.amitracker.database.repository.GuildRepository;
import enterprises.iwakura.amitracker.database.repository.UserRepository;
import enterprises.iwakura.amitracker.service.ConcurrencyService;
import enterprises.iwakura.amitracker.service.ProductService;
import enterprises.iwakura.amitracker.util.URLHelper;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

@Slf4j
@Bean
@RequiredArgsConstructor
public class DiscordListener extends ListenerAdapter {

    private final GuildRepository guildRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final ConcurrencyService concurrencyService;
    private final ProductCommand productCommand;

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

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var message = event.getMessage();
        var channel = event.getChannel();
        var user = event.getMessage().getAuthor();

        if (!channel.canTalk() || user.isBot()) {
            return;
        }

        var productCode = URLHelper.extractProductCode(message.getContentRaw(), false);
        if (productCode != null) {
            log.info("User {} specified AmiAmi product code {} in their message, fetching and responding with product info...",
                user.getIdLong(), productCode
            );
            channel.sendTyping().complete();

            concurrencyService.scheduleThrottled(channel.getId(), () -> {
                productService.getOrQueryProduct(productCode).ifPresent(e -> {
                    var messageBuilder = productCommand.createProductMessage(e).build();
                    message.reply(MessageCreateData.fromEditData(messageBuilder)).queue();
                });
            });
        }
    }
}
