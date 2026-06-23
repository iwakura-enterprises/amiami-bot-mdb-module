package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import enterprises.iwakura.amitracker.constant.Constants;
import enterprises.iwakura.amitracker.object.ProductSearchParameters;
import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import enterprises.iwakura.kirara.amiami.request.AmiAmiSearchRequest;
import enterprises.iwakura.kirara.amiami.request.AmiAmiSearchRequest.SortKeys;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "product_query")
public class ProductListQueryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    /**
     * The maximum number of pages to paginate through when performing the product query. Usually this should be
     * just 1. The usual amount of products per page is {@link AmiAmiApiService#MAX_ITEMS_PER_QUERY}.
     */
    @Column(nullable = false)
    private int maxPagination = Constants.DEFAULT_PRODUCT_LIST_QUERY_PAGINATION;

    /**
     * Whenever the next product added/removed to list changes should be ignored. Usually used when changing the
     * max pagination.
     */
    @Column(nullable = false)
    private boolean skipNextProductAddOrRemoveChangeAnnouncements = false;

    /**
     * Determines whenever this product list query is a global template. Templates will
     * not be deleted when the last ChannelProductListQueryEntity gets deleted. Also,
     * templates are shown in the /search-notification template command
     */
    private boolean globalTemplate;

    // Search params
    @Embedded
    private ProductSearchParameters productSearchParameters;

    @Column(nullable = false)
    private long totalItemsCount = 0L;

    private OffsetDateTime lastQueryAt;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseJson;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "productListQuery", fetch = FetchType.LAZY)
    private List<ProductListQueryResultEntryEntity> entries;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "productListQuery", fetch = FetchType.LAZY)
    private List<ChannelProductListQueryEntity> channelsWithQuery;

    public AmiAmiSearchRequest toAmiAmiSearchRequest(int page) {
        if (productSearchParameters == null) {
            return AmiAmiSearchRequest.builder()
                .maximumItemsPerPage(AmiAmiApiService.MAX_ITEMS_PER_QUERY)
                .pageNumber(page)
                .sortKey(SortKeys.RECENTLY_UPDATED)
                .searchKeywords("") // Empty search keyword
                .build();
        } else {
            return AmiAmiSearchRequest.builder()
                .maximumItemsPerPage(AmiAmiApiService.MAX_ITEMS_PER_QUERY)
                .pageNumber(page)
                .sortKey(SortKeys.RECENTLY_UPDATED)
                .searchKeywords(productSearchParameters.getSearchKeywords())
                .filterAmiAmiBonus(productSearchParameters.getFilterAmiAmiBonus())
                .filterOnSaleItems(productSearchParameters.getFilterOnSaleItems())
                .category1Id(productSearchParameters.getCategory1Id())
                .category2Id(productSearchParameters.getCategory2Id())
                .category3Id(productSearchParameters.getCategory3Id())
                .category4Id(productSearchParameters.getCategory4Id())
                .categoryTagId(productSearchParameters.getCategoryTagId())
                .characterNameId(productSearchParameters.getCharacterNameId())
                .makerId(productSearchParameters.getMakerId())
                .originalTitleId(productSearchParameters.getOriginalTitleId())
                .seriesTitleId(productSearchParameters.getSeriesTitleId())
                .build();
        }
    }
}
