package enterprises.iwakura.amitracker.service.proxy;

import java.net.Proxy.Type;
import java.util.List;
import java.util.Objects;

import enterprises.iwakura.amitracker.constant.AnonymityLevel;
import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.kirara.core.Serializer;
import enterprises.iwakura.kirara.gson.GsonSerializer;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class ProxyScrapeProxyFetcher extends JsonProxyFetcher {

    private static final String URL = "https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies&proxy_format=ipport&format=json";

    @Override
    protected Serializer getSerializer() {
        return new GsonSerializer();
    }

    @Override
    public List<ProxyDTO> fetch() {
        var response = kirara.createRequest("GET", URL, Response.class).send().join();

        return response.proxies().stream()
            .filter(Objects::nonNull)
            .map(proxy -> ProxyDTO.builder()
                .protocol(parseProtocol(proxy.protocol()))
                .ip(proxy.ip())
                .port(proxy.port())
                .anonymityLevel(parseAnonymity(proxy.anonymity()))
                .timesAlive(proxy.times_alive())
                .timesDead(proxy.times_dead())
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

    @Override
    public String toString() {
        return "ProxyScrapeProxyFetcher{url='" + URL + "'}";
    }

    private record Response(int shown_records, int total_records, List<Proxy> proxies) {}

    private record Proxy(
        String proxy,
        String ip,
        int port,
        String protocol,
        String anonymity,
        Integer times_alive,
        Integer times_dead
    ) {}
}