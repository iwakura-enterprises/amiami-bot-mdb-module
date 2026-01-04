package enterprises.iwakura.amitracker.service;

import com.google.gson.Gson;

import enterprises.iwakura.kirara.amiami.AmiAmiApi;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
public class AmiAmiApiService extends AmiAmiApi {

    public static final Long MAX_ITEMS_PER_QUERY = 50L;

    public AmiAmiApiService(Gson gson) {
        super(gson);
    }
}
