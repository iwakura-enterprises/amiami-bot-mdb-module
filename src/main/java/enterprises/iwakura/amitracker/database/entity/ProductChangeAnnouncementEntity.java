package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import enterprises.iwakura.amitracker.constant.QueueState;
import enterprises.iwakura.amitracker.constant.ProductChangeType;
import enterprises.iwakura.amitracker.object.ProductChangeHolder;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_change_announcement")
public class ProductChangeAnnouncementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Enumerated(value = EnumType.STRING)
    private QueueState announcementState;

    private String sendLog;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProductChangeType> productChangeTypes;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "wishlist_id")
    private WishlistEntity wishlist;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "channelProductListQueryEntity_id")
    private ChannelProductListQueryEntity channelProductListQuery;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JdbcTypeCode(SqlTypes.JSON)
    private ProductChangeHolder productChangeHolder;

    /**
     * Appends message to the send log, separating messages with new lines
     *
     * @param message Message
     */
    public void appendToSendLog(String message) {
        if (sendLog == null) {
            sendLog = message;
        } else {
            sendLog += "\n" + message;
        }
    }
}
