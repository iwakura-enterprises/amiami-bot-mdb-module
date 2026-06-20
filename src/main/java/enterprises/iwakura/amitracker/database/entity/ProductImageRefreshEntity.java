package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;

import enterprises.iwakura.amitracker.constant.QueueState;
import enterprises.iwakura.amitracker.constant.ImageRefreshReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_image_refresh")
public class ProductImageRefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    private ProductEntity product;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private QueueState state;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ImageRefreshReason refreshReason;

    @Column(nullable = false)
    private OffsetDateTime refreshAfter;

    private Integer retryNumber;

}
