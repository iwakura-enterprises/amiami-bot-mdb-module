package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import enterprises.iwakura.amitracker.constant.ProductState;
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
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_history")
public class ProductHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    /**
     * Lowest price in JPY at the specified time. Items that are Pre-Owned
     * may include more prices due to more offerings.
     */
    @Column(nullable = false)
    private Long priceJpy;

    /**
     * The state of the product at the specified time.
     */
    @Enumerated(EnumType.STRING)
    private ProductState productState;

    @JdbcTypeCode(SqlTypes.JSON)
    private String responseJson; // Mixed ResultItem and Item detail

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

}
