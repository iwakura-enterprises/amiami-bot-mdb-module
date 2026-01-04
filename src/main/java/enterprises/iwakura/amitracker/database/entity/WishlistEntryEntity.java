package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wishlist_entry")
public class WishlistEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wishlist_id")
    private WishlistEntity wishlist;


    // NOTE: No lastQueriedAt -> check product.lastUpdatedAt instead
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

}
