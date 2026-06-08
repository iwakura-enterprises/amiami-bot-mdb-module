package enterprises.iwakura.amitracker.database.entity;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import enterprises.iwakura.kirara.amiami.request.AmiAmiSearchRequest;
import enterprises.iwakura.kirara.amiami.request.AmiAmiSearchRequest.SortKeys;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private Integer maxPagination;

    /**
     * Whenever the next product added/removed to list changes should be ignored. Usually used when changing the
     * max pagination.
     */
    private boolean skipNextProductAddOrRemoveChangeAnnouncements;

    // Search params
    private String searchKeywords;
    private Boolean filterAnyAvailability;
    private Boolean filterPreOrder;
    private Boolean filterBackOrder;
    private Boolean filterNewItems;
    private Boolean filterPreOwnedItems;
    private Boolean filterOnSaleItems;
    private Integer category1Id;
    private Integer category2Id;
    private Integer category3Id;
    private Integer category4Id;
    private Integer characterNameId;
    private Integer makerId;
    private Integer originalTitleId;
    private Integer seriesTitleId;

    @Column(nullable = false)
    private Long totalItemsCount;

    private OffsetDateTime lastQueryAt;

    @OneToMany(mappedBy = "productListQuery", fetch = FetchType.LAZY)
    private List<ProductListQueryResultEntryEntity> entries;

    @OneToMany(mappedBy = "productListQuery", fetch = FetchType.LAZY)
    private List<ChannelProductListQueryEntity> channelsWithQuery;

    public AmiAmiSearchRequest toAmiAmiSearchRequest(int page) {
        return AmiAmiSearchRequest.builder()
            .maximumItemsPerPage(AmiAmiApiService.MAX_ITEMS_PER_QUERY)
            .pageNumber(page)
            .sortKey(SortKeys.RECENTLY_UPDATED)
            .searchKeywords(searchKeywords)
            .filterAnyAvailability(filterAnyAvailability)
            .filterPreOrder(filterPreOrder)
            .filterBackOrder(filterBackOrder)
            .filterNewItems(filterNewItems)
            .filterPreOwnedItems(filterPreOwnedItems)
            .filterOnSaleItems(filterOnSaleItems)
            .category1Id(category1Id)
            .category2Id(category2Id)
            .category3Id(category3Id)
            .category4Id(category4Id)
            .characterNameId(characterNameId)
            .makerId(makerId)
            .originalTitleId(originalTitleId)
            .seriesTitleId(seriesTitleId)
            .build();
    }
}
