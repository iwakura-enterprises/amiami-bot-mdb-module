package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import enterprises.iwakura.amitracker.constant.Constants;
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
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wishlist")
public class WishlistEntity {

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

    @Column(nullable = false)
    private boolean stockChangeEnabled = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @OneToMany(mappedBy = "wishlist")
    private List<WishlistEntryEntity> entries = new ArrayList<>();

    @OneToMany(mappedBy = "wishlist", fetch = FetchType.LAZY)
    private List<ProductChangeAnnouncementEntity> productChangeAnnouncements;

    /**
     * Creates a default placeholder wishlist entity.
     *
     * @return a WishlistEntity with the default name
     */
    public static WishlistEntity createDefault() {
        var wishlist = new WishlistEntity();
        wishlist.setName(Constants.DEFAULT_WISHLIST_NAME);
        wishlist.setPriceDiscountEnabled(true);
        wishlist.setStockChangeEnabled(true);
        return wishlist;
    }

    /**
     * Checks if the wishlist contains a product with the specified product code.
     *
     * @param productCode the product code to check
     * @return true if the product is in the wishlist, false otherwise
     */
    public boolean containsProductCode(String productCode) {
        return ListUtils.emptyIfNull(entries)
            .stream()
            .anyMatch(entry -> entry.getProduct().getCode().equalsIgnoreCase(productCode));
    }
}
