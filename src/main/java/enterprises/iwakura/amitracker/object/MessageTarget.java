package enterprises.iwakura.amitracker.object;

import java.util.Objects;
import java.util.Set;

import enterprises.iwakura.amitracker.database.entity.GuildEntity;
import lombok.Data;

@Data
public class MessageTarget {

    private GuildEntity guild;

    private boolean isUser;
    private long targetId;
    private long guildId;
    private Set<Long> rolesToPing;
    private boolean pingsCleared;

    public MessageTarget(long userId) {
        this.isUser = true;
        this.targetId = userId;
    }

    public MessageTarget(long channelId, GuildEntity guild, Set<Long> rolesToPing, boolean pingsCleared) {
        this.isUser = false;
        this.targetId = channelId;
        this.guild = guild;
        this.guildId = guild.getId();
        this.rolesToPing = rolesToPing;
        this.pingsCleared = pingsCleared;
    }

    public boolean isForGuild() {
        return !isUser;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MessageTarget that)) {
            return false;
        }
        return isUser == that.isUser && targetId == that.targetId && guildId == that.guildId
            && Objects.equals(rolesToPing, that.rolesToPing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isUser, targetId, rolesToPing, guildId);
    }
}
