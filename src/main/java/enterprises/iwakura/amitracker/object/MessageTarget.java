package enterprises.iwakura.amitracker.object;

import java.util.Set;

import lombok.Data;

@Data
public class MessageTarget {

    private boolean isUser;
    private long targetId;
    private Set<Long> rolesToPing;

    private long guildId;

    public MessageTarget(long userId) {
        this.isUser = true;
        this.targetId = userId;
    }

    public MessageTarget(long channelId, long guildId, Set<Long> rolesToPing) {
        this.isUser = false;
        this.targetId = channelId;
        this.guildId = guildId;
        this.rolesToPing = rolesToPing;
    }

    public boolean isForGuild() {
        return !isUser;
    }
}
