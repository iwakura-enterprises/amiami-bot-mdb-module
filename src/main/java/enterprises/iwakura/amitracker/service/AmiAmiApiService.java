package enterprises.iwakura.amitracker.service;

import com.google.gson.Gson;

import enterprises.iwakura.kirara.amiami.AmiAmiApi;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
public class AmiAmiApiService extends AmiAmiApi {

    public static final String AMIAMI_URL = "https://www.amiami.com";
    public static final String NO_IMAGE_URL = "/images/product/main/noimage_e.jpg";
    public static final Long MAX_ITEMS_PER_QUERY = 50L;

    public AmiAmiApiService(Gson gson) {
        super(gson);
    }

    /**
     * Creates a URL to the AmiAmi product detail page for the given product code.
     *
     * @param productCode the product code
     *
     * @return the URL to the AmiAmi product detail page
     */
    public String createAmiAmiProductDetailUrl(String productCode) {
        return String.format("%s/eng/detail/?gcode=%s", AMIAMI_URL, productCode);
    }
}
