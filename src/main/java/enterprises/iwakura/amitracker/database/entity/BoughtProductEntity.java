package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import enterprises.iwakura.amitracker.constant.Currency;
import jakarta.persistence.Column;
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bought_product")
public class BoughtProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime boughtAt;

    @Column(nullable = false)
    private Long priceJpy;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    /**
     * Constructor to create a BoughtProductEntity with specified details.
     *
     * @param userEntity    User who bought the product
     * @param productEntity Product that was bought
     * @param boughtAt      Date and time when the product was bought
     * @param priceJpy      Price in JPY
     * @param currency      Currency used for the purchase
     */
    public BoughtProductEntity(
        UserEntity userEntity,
        ProductEntity productEntity,
        OffsetDateTime boughtAt,
        Long priceJpy,
        Currency currency
    ) {
        this.user = userEntity;
        this.product = productEntity;
        this.boughtAt = boughtAt;
        this.priceJpy = priceJpy;
        this.currency = currency;
    }
}
