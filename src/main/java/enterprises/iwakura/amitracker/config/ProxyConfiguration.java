package enterprises.iwakura.amitracker.config;

import lombok.Data;

@Data
public class ProxyConfiguration {

    private boolean proxyRequestsEnabled = true;
    private boolean proxyFetchEnabled = true;
    private boolean proxyProbeEnabled = true;

    /**
     * Number of retries when sending queries in OkHttpProxyHttpCore
     */
    private int httpCoreRetry = 3;

    /**
     * Maximum number of proxies to probe at once
     */
    private int maxPerProbe = 100;

    /**
     * The reliability ratio to set proxy to used up
     */
    private double usedUpReliabilityRatio = 0.3;

    /**
     * The number of threads to schedule probes from
     */
    private int probeThreads = 20;

}
