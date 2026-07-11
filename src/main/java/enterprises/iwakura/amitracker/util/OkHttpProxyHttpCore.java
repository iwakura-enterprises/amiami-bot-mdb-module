package enterprises.iwakura.amitracker.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import enterprises.iwakura.amitracker.object.ProxyDTO;
import enterprises.iwakura.amitracker.service.ConfigurationService;
import enterprises.iwakura.amitracker.service.ProxyService;
import enterprises.iwakura.kirara.core.ApiRequest;
import enterprises.iwakura.kirara.core.HttpCore;
import enterprises.iwakura.kirara.core.Kirara;
import enterprises.iwakura.kirara.core.RequestHeader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implementation of the {@link HttpCore} using OkHttp.
 */
@Slf4j
@Getter
@Setter
public class OkHttpProxyHttpCore extends HttpCore {

    private final ConfigurationService configurationService;
    /**
     * The OkHttpClient instance used to send requests.
     */
    private final OkHttpClient okHttpClient;
    private final ProxyService proxyService;
    private final ProxyDTO proxyOverride;

    /**
     * Creates OkHttpProxyHttpCore
     *
     * @param configurationService Configuration service
     * @param proxyService         Proxy service
     * @param okHttpClient         OkHttp client to use
     * @param proxyOverride        Proxy override
     */
    public OkHttpProxyHttpCore(
        ConfigurationService configurationService,
        ProxyService proxyService,
        OkHttpClient okHttpClient,
        ProxyDTO proxyOverride
    ) {
        this.configurationService = configurationService;
        this.okHttpClient = okHttpClient;
        this.proxyService = proxyService;
        this.proxyOverride = proxyOverride;
    }

    /**
     * Creates a new Request.Builder instance.
     * This method can be overridden to customize the Request.Builder creation.
     *
     * @return a new Request.Builder instance
     */
    protected Request.Builder createRequestBuilder() {
        return new Request.Builder();
    }

    @Override
    public Executor getExecutor() {
        if (proxyOverride != null) {
            return Runnable::run;
        }
        return super.getExecutor();
    }

    @Override
    public <T> CompletableFuture<T> send(ApiRequest<T> request, Executor executor) {
        final var kirara = request.getKirara();
        final var url = request.computeRequestUrl();
        final var method = request.getMethod();
        final var headers = request.getHeaders();
        final var body = request.getBody();
        final var responseClass = request.getResponseClass();
        final var future = new CompletableFuture<T>();
        final var requestBuilder = createRequestBuilder();

        if (executor == null) {
            executor = getExecutor();
        }

        executor.execute(() -> {
            var proxyConfig = configurationService.getProxyConfiguration();

            try {
                requestBuilder.url(url);
                requestBuilder.method(method, getRequestBody(kirara, request, body));

                if (headers != null) {
                    RequestHeader.convertToMap(headers).forEach(requestBuilder::header);
                }

                kirara.onRequest(request);

                var retries = proxyOverride == null ? proxyConfig.getHttpCoreRetry() : 0;
                Throwable finalException = null;
                List<ProxyDTO> triedProxies = new ArrayList<>(retries);

                for (int retry = 0; retry <= retries; retry++) {
                    ProxyDTO proxy;

                    if (proxyOverride != null) {
                        proxy = proxyOverride;
                    } else if (retry == retries || !proxyConfig.isProxyRequestsEnabled()) {
                        if (proxyConfig.isProxyRequestsEnabled()) {
                            log.warn("Tried {} times to use proxies {} but failed! Using direct connection @ {}",
                                retries,
                                triedProxies.stream().map(ProxyDTO::getId).toList(),
                                request.computeRequestUrl()
                            );
                        }
                        proxy = ProxyDTO.NO_PROXY;
                    } else {
                        proxy = proxyService.pick();
                        triedProxies.add(proxy);
                    }
                    var client = okHttpClient.newBuilder()
                        .proxy(proxy.toJavaProxy())
                        .build();
                    var startMillis = System.currentTimeMillis();

                    try (Response okHttpResponse = client.newCall(requestBuilder.build()).execute()) {
                        var endMillis = System.currentTimeMillis() - startMillis;

                        final var responseHeaders = okHttpResponse.headers().toMultimap();
                        final var responseStatusCode = okHttpResponse.code();
                        final var responseBytes = okHttpResponse.body().bytes();
                        final var response = convertBytesToResponse(kirara, request, responseBytes, responseClass, responseStatusCode, responseHeaders);

                        kirara.onResponse(request, response);

                        future.complete(handleKiraraSupportedResponse(kirara, response));
                        finalException = null;
                        proxy.updateLatency((int) endMillis);
                        proxy.addTimesAlive();
                        break; // from retry loop
                    } catch (Throwable throwable) {
                        if (proxyOverride == null) {
                            log.warn("Request failed, retry {}/{} via proxy[{}] -> {}, {}",
                                retry + 1, retries, proxy.getId(), request.computeRequestUrl(), throwable.toString()
                            );
                        }
                        finalException = throwable;
                        // TODO: Nastavit na not ready pokud to třeba 10x failne nebo něco takového
                        proxy.addTimesDead();
                        proxy.setLastError(throwable.toString());
                    }
                }

                if (finalException != null) {
                    kirara.onException(request, finalException);
                    future.completeExceptionally(finalException);
                }

                if (proxyOverride == null) {
                    try {
                        proxyService.scheduleUpdateProxies(triedProxies);
                    } catch (Exception exception) {
                        log.error("Failed to update proxies while sending HttpCore request!", exception);
                    }
                }
            } catch (Throwable exception) {
                kirara.onException(request, exception);
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    @Override
    public void close() {
        if (okHttpClient != null) {
            okHttpClient.dispatcher().executorService().shutdown();
            okHttpClient.connectionPool().evictAll();
        }
    }

    /**
     * Gets a RequestBody for the request body.
     *
     * @param kirara  the Kirara instance associated with the request
     * @param request the API request being sent
     * @param body    the body of the request, which can be null, a byte array, a String, or any other object
     *
     * @return an OkHttp RequestBody, or null if there is no body
     */
    protected RequestBody getRequestBody(Kirara kirara, ApiRequest<?> request, Object body) {
        if (body == null) {
            // OkHttp requires non-null body for POST/PUT/PATCH, null for GET/HEAD/DELETE
            final var method = request.getMethod();
            if (method.equals("GET") || method.equals("HEAD") || method.equals("DELETE")) {
                return null;
            }
            return RequestBody.create(new byte[0]);
        }

        return RequestBody.create(convertBodyToBytes(kirara, request, body));
    }
}