package enterprises.iwakura.amitracker.service.proxy;

import java.net.Proxy.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.kirara.core.Serializer;
import enterprises.iwakura.kirara.core.impl.StringSerializer;

public class SimpleProxyFetcher extends BaseProxyFetcher {

    private final Pattern LINE_PATTERN = Pattern.compile("^(\\d{1,3}(?:\\.\\d{1,3}){3}):(\\d+)(?::(.+))?$");
    private final Type protocol;
    private final String url;

    public static final Map<String, String> aliases = Map.of(
        "czechia", "CZ",
        "czech republic", "CZ",
        "south korea", "KR",
        "north korea", "KP",
        "russia", "RU",
        "vietnam", "VN",
        "iran", "IR",
        "syria", "SY",
        "taiwan", "TW",
        "uk", "GB"
    );

    public SimpleProxyFetcher(Type protocol, String url) {
        this.protocol = protocol;
        this.url = url;
    }

    @Override
    protected Serializer getSerializer() {
        return new StringSerializer();
    }

    @Override
    public List<ProxyDTO> fetch() {
        var result = kirara.createRequest("GET", url, String.class).send().join();

        return result.lines().map(line -> {
            var matcher = LINE_PATTERN.matcher(line);
            if (matcher.matches()) {
                var ip = matcher.group(1);
                var port = matcher.group(2);
                var country = matcher.group(3);

                return ProxyDTO.builder()
                    .protocol(protocol)
                    .ip(ip)
                    .port(Integer.parseInt(port))
                    .countyCode(tryFindCountryCode(country))
                    .responseData(line)
                    .build();
            }
            return null;
        }).filter(Objects::nonNull).toList();
    }

    private String tryFindCountryCode(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        String normalized = country.trim();

        String alias = aliases.get(normalized.toLowerCase(Locale.ROOT));
        if (alias != null) {
            return alias;
        }

        for (String code : Locale.getISOCountries()) {
            Locale locale = new Locale("", code);
            if (locale.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(normalized)
                || locale.getDisplayCountry(Locale.ROOT).equalsIgnoreCase(normalized)) {
                return code;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SimpleProxyFetcher{" +
            "url='" + url + '\'' +
            "}";
    }
}
