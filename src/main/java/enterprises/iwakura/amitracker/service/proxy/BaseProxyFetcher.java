package enterprises.iwakura.amitracker.service.proxy;

import static enterprises.iwakura.amitracker.service.ProxyService.CONNECT_TIMEOUT_SECONDS;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.kirara.core.ApiRequest;
import enterprises.iwakura.kirara.core.HttpCore;
import enterprises.iwakura.kirara.core.Kirara;
import enterprises.iwakura.kirara.core.Serializer;
import enterprises.iwakura.kirara.httpclient.HttpClientHttpCore;

public abstract class BaseProxyFetcher {

    protected DummyKirara kirara;

    public BaseProxyFetcher() {
        kirara = new DummyKirara(getSerializer());
        kirara.setApiUrl("");
    }

    protected abstract Serializer getSerializer();

    public abstract List<ProxyDTO> fetch();

    public static class DummyKirara extends Kirara {

        public DummyKirara(Serializer serializer) {
            super(new HttpClientHttpCore(HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.of(CONNECT_TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                .build()
            ), serializer);
        }

        @Override
        public <R extends ApiRequest<T>, T> R createRequest(String method, String endpoint, Class<T> responseClass) {
            return super.createRequest(method, endpoint, responseClass);
        }
    }
}
