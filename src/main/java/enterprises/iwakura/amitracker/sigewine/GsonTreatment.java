package enterprises.iwakura.amitracker.sigewine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import enterprises.iwakura.sigewine.core.annotations.Bean;

public class GsonTreatment {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }
}
