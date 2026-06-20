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
import lombok.NoArgsConstructor;

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

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductEntity productEntity;

    @ManyToOne
    @JoinColumn(name = "wishlist_id")
    private WishlistEntity wishlist;

    @ManyToOne
    @JoinColumn(name = "channelProductListQueryEntity_id")
    private ChannelProductListQueryEntity channelProductListQuery;

    @JdbcTypeCode(SqlTypes.JSON)
    private ProductChangeHolder productChangeHolder;
}
