package enterprises.iwakura.amitracker.service;

import java.util.List;

import com.google.gson.Gson;

import enterprises.iwakura.amitracker.util.OkHttpProxyHttpCore;
import enterprises.iwakura.kirara.amiami.AmiAmiApi;
import enterprises.iwakura.kirara.gson.GsonSerializer;
import enterprises.iwakura.kirara.httpclient.HttpClientHttpCore;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

@Bean
@Slf4j
public class AmiAmiApiService extends AmiAmiApi {

    public static final String AMIAMI_URL = "https://www.amiami.com";
    public static final String NO_IMAGE_URL = "/images/product/main/noimage_e.jpg";
    public static final Long MAX_ITEMS_PER_QUERY = 50L;

    public AmiAmiApiService(
        ConfigurationService configurationService,
        ProxyService proxyService,
        Gson gson
    ) {
        super(
            DEFAULT_API_URL,
            DEFAULT_IMAGE_API_URL,
            DEFAULT_AMIAMI_URL,
            new OkHttpProxyHttpCore(
                configurationService,
                proxyService,
                new OkHttpClient.Builder()
                    .protocols(List.of(Protocol.HTTP_1_1))
                    .build(),
                null
            ),
            new GsonSerializer(gson, SUPPORTED_CONTENT_TYPES)
        );
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
