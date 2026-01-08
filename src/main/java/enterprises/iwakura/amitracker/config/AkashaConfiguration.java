package enterprises.iwakura.amitracker.config;

import lombok.Data;

@Data
public class AkashaConfiguration {

    private String url = "https://akasha.iwakura.enterprises";
    private String token = "";
    private String datasource = "";
    private String productImagePath = "public/amitracker/product/%s.jpg";

}
