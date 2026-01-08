package enterprises.iwakura.amitracker.database.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import enterprises.iwakura.amitracker.constant.ProductState;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    private OffsetDateTime lastImageUpdateAt;

    /**
     * Latest lowest price in JPY. Items that are Pre-Owned may include more prices due to
     * more offerings.
     */
    private Long priceJpy;

    private String makerName;

    @Enumerated(EnumType.STRING)
    private ProductState productState;

    private LocalDate releaseDate;

    /**
     * History for the product.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "product")
    private List<ProductHistoryEntity> history = new ArrayList<>();
}
