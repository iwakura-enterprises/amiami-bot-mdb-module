package enterprises.iwakura.amitracker.object;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import enterprises.iwakura.amitracker.service.AmiAmiApiService;
import lombok.Data;

@Data
public class ProductSearchParameters {

    public static final ProductSearchParameters EMPTY = new ProductSearchParameters();

    private String searchKeywords;
    private Boolean filterAmiAmiBonus;
    private Boolean filterOnSaleItems;
    private Integer category1Id;
    private Integer category2Id;
    private Integer category3Id;
    private Integer category4Id;
    private Integer categoryTagId;
    private Integer characterNameId;
    private Integer makerId;
    private Integer originalTitleId;
    private Integer seriesTitleId;

    /**
     * Parses search parameters from the URL
     *
     * @param searchUrl Search URL
     *
     * @return ProductSearchParameters
     */
    public static ProductSearchParameters parseFromUrl(String searchUrl) {
        if (searchUrl == null || searchUrl.isBlank()) {
            return EMPTY;
        }

        var params = new ProductSearchParameters();
        var query = searchUrl.contains("?") ? searchUrl.split("\\?", 2)[1] : searchUrl;
        for (var pair : query.split("&")) {
            var kv = pair.split("=", 2);
            if (kv.length < 2) {
                continue;
            }
            var key = kv[0];
            var value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            switch (key) {
                case "s_keywords" -> params.setSearchKeywords(value);
                case "s_st_list_store_bonus" -> params.setFilterAmiAmiBonus("1".equals(value));
                case "s_st_saleitem" -> params.setFilterOnSaleItems("1".equals(value));
                case "s_cate1" -> params.setCategory1Id(Integer.parseInt(value));
                case "s_cate2" -> params.setCategory2Id(Integer.parseInt(value));
                case "s_cate3" -> params.setCategory3Id(Integer.parseInt(value));
                case "s_cate4" -> params.setCategory4Id(Integer.parseInt(value));
                case "s_cate_tag" -> params.setCategoryTagId(Integer.parseInt(value));
                case "s_charaname_search_id" -> params.setCharacterNameId(Integer.parseInt(value));
                case "s_maker_id" -> params.setMakerId(Integer.parseInt(value));
                case "s_originaltitle_id" -> params.setOriginalTitleId(Integer.parseInt(value));
                case "s_seriestitle_id" -> params.setSeriesTitleId(Integer.parseInt(value));
            }
        }

        return params;
    }

    public String toDiscordMessage() {
        if (isEmpty()) {
            return "No filters set.";
        }

        var sb = new StringBuilder();
        if (searchKeywords != null && !searchKeywords.isBlank())
            sb.append("Keywords: ").append(searchKeywords).append("\n");
        if (filterAmiAmiBonus != null)
            sb.append("AmiAmi Bonus: ").append(filterAmiAmiBonus).append("\n");
        if (filterOnSaleItems != null)
            sb.append("On Sale Items: ").append(filterOnSaleItems).append("\n");
        if (category1Id != null)
            sb.append("Category 1: ").append(category1Id).append("\n");
        if (category2Id != null)
            sb.append("Category 2: ").append(category2Id).append("\n");
        if (category3Id != null)
            sb.append("Category 3: ").append(category3Id).append("\n");
        if (category4Id != null)
            sb.append("Category 4: ").append(category4Id).append("\n");
        if (categoryTagId != null)
            sb.append("Category Tag: ").append(categoryTagId).append("\n");
        if (characterNameId != null)
            sb.append("Character Name: ").append(characterNameId).append("\n");
        if (makerId != null)
            sb.append("Maker: ").append(makerId).append("\n");
        if (originalTitleId != null)
            sb.append("Original Title: ").append(originalTitleId).append("\n");
        if (seriesTitleId != null)
            sb.append("Series Title: ").append(seriesTitleId).append("\n");

        return sb.toString().stripTrailing();
    }

    public String toUrl() {
        if (isEmpty()) {
            return "%s/eng/search/list/".formatted(AmiAmiApiService.AMIAMI_URL);
        }

        var sb = new StringBuilder("%s/eng/search/list/?".formatted(AmiAmiApiService.AMIAMI_URL));
        if (searchKeywords != null && !searchKeywords.isBlank())
            sb.append("s_keywords=").append(URLEncoder.encode(searchKeywords, StandardCharsets.UTF_8)).append("&");
        if (filterAmiAmiBonus != null && filterAmiAmiBonus)
            sb.append("s_st_list_store_bonus=1&");
        if (filterOnSaleItems != null && filterOnSaleItems)
            sb.append("s_st_saleitem=1&");
        if (category1Id != null)
            sb.append("s_cate1=").append(category1Id).append("&");
        if (category2Id != null)
            sb.append("s_cate2=").append(category2Id).append("&");
        if (category3Id != null)
            sb.append("s_cate3=").append(category3Id).append("&");
        if (category4Id != null)
            sb.append("s_cate4=").append(category4Id).append("&");
        if (categoryTagId != null)
            sb.append("s_cate_tag=").append(categoryTagId).append("&");
        if (characterNameId != null)
            sb.append("s_charaname_search_id=").append(characterNameId).append("&");
        if (makerId != null)
            sb.append("s_maker_id=").append(makerId).append("&");
        if (originalTitleId != null)
            sb.append("s_originaltitle_id=").append(originalTitleId).append("&");
        if (seriesTitleId != null)
            sb.append("s_seriestitle_id=").append(seriesTitleId).append("&");

        if (sb.charAt(sb.length() - 1) == '&') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    public boolean isEmpty() {
        return (searchKeywords == null || searchKeywords.isBlank())
            && filterAmiAmiBonus == null
            && filterOnSaleItems == null
            && category1Id == null
            && category2Id == null
            && category3Id == null
            && category4Id == null
            && categoryTagId == null
            && characterNameId == null
            && makerId == null
            && originalTitleId == null
            && seriesTitleId == null;
    }
}
