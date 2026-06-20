package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.jetbrains.annotations.NotNull;

import enterprises.iwakura.amitracker.constant.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "guild")
public class GuildEntity {

    @Id
    @Column(nullable = false)
    private Long id;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @NotNull
    @Column(nullable = false)
    private String name;

    @Enumerated(value = EnumType.STRING)
    private Currency preferredCurrency = Currency.JPY;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Currency> secondaryCurrencies = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "guild")
    private List<ChannelEntity> channels;
}
