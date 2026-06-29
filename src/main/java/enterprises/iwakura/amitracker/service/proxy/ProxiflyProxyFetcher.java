package enterprises.iwakura.amitracker.service.proxy;

import java.net.Proxy.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;

import enterprises.iwakura.amitracker.constant.AnonymityLevel;
import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.kirara.core.Serializer;
import enterprises.iwakura.kirara.core.impl.StringSerializer;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProxiflyProxyFetcher extends JsonProxyFetcher {

    private static final String URL = "https://raw.githubusercontent.com/proxifly/free-proxy-list/refs/heads/main/proxies/all/data.json";
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
                .ip(proxy.ip())
                .port(proxy.port())
                .anonymityLevel(parseAnonymity(proxy.anonymity()))
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

    private AnonymityLevel parseAnonymity(String anonymity) {
        if (anonymity == null) {
            return null;
        }
        return switch (anonymity.toLowerCase()) {
            case "elite" -> AnonymityLevel.ELITE;
            case "anonymous" -> AnonymityLevel.ANONYMOUS;
            case "transparent" -> AnonymityLevel.TRANSPARENT;
            default -> null;
        };
    }

    private String parseCountry(Geolocation geo) {
        if (geo == null || geo.country() == null || geo.country().equals("ZZ")) {
            return null;
        }
        return geo.country();
    }

    @Override
    public String toString() {
        return "ProxiflyProxyFetcher{url='" + URL + "'}";
    }

    private record Proxy(
        String proxy,
        String protocol,
        String ip,
        int port,
        String anonymity,
        Geolocation geolocation
    ) {}

    private record Geolocation(String country, String city) {}
}