package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import enterprises.iwakura.amitracker.constant.ProductState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "channel_product_list_query")
public class ChannelProductListQueryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean priceDiscountEnabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProductState> stateToEnabled = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProductState> stateFromEnabled = new ArrayList<>();

    @Column(nullable = false)
    private boolean newProductsEnabled = true;

    @Column(nullable = false, columnDefinition = "bigint[]")
    private Set<Long> roleIdsToNotify = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "channel_id")
    private ChannelEntity channel;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "productListQuery_id")
    private ProductListQueryEntity productListQuery;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "channelProductListQuery", fetch = FetchType.LAZY)
    private List<ProductChangeAnnouncementEntity> productChangeAnnouncements;
}
