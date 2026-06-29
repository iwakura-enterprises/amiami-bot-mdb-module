package enterprises.iwakura.amitracker.service.proxy;

import java.net.Proxy.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;

import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.kirara.core.Serializer;
import enterprises.iwakura.kirara.core.impl.StringSerializer;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class MonosansProxyFetcher extends JsonProxyFetcher {

    private static final String URL = "https://raw.githubusercontent.com/monosans/proxy-list/refs/heads/main/proxies_pretty.json";
    private static final Gson GSON = new Gson();

    @Override
    protected Serializer getSerializer() {
        return new StringSerializer();
    }

    @Override
    public List<ProxyDTO> fetch() {
        var raw = kirara.createRequest("GET", URL, String.class).send().join();
        var proxies = GSON.fromJson(raw, Proxy[].class);

        return Arrays.stream(proxies)
            .filter(Objects::nonNull)
            .map(proxy -> ProxyDTO.builder()
                .protocol(parseProtocol(proxy.protocol()))
                .ip(proxy.host())
                .port(proxy.port())
                .countyCode(parseCountry(proxy.geolocation()))
                .build()
            )
            .filter(dto -> dto.getProtocol() != null)
            .toList();
    }

    private Type parseProtocol(String protocol) {
        if (protocol == null) {
            return null;
        }
        return switch (protocol.toLowerCase()) {
            case "http", "https" -> Type.HTTP;
            case "socks4", "socks5" -> Type.SOCKS;
            default -> null;
        };
    }

    private String parseCountry(Geolocation geo) {
        if (geo == null || geo.country() == null) {
            return null;
        }
        return geo.country().iso_code();
    }

    @Override
    public String toString() {
        return "MonosansProxyFetcher{url='" + URL + "'}";
    }

    private record Proxy(
        String protocol,
        String host,
        int port,
        Geolocation geolocation
    ) {}

    private record Geolocation(Country country) {}

    private record Country(String iso_code) {}
}