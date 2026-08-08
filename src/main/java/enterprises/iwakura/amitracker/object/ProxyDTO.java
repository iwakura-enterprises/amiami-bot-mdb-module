package enterprises.iwakura.amitracker.object;

import java.net.InetSocketAddress;
import java.net.Proxy;

import enterprises.iwakura.amitracker.constant.AnonymityLevel;
import enterprises.iwakura.amitracker.constant.ProxyState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProxyDTO {

    public static final ProxyDTO NO_PROXY = ProxyDTO.builder().id(-1225L).build();

    private Long id;

    private Proxy.Type protocol;
    private String ip;
    private int port;
    private ProxyState state;

    private Integer latencyMillis;
    private Integer timesAlive;
    private Integer timesDead;
    private Integer timesDeadInRow;
    private String countyCode;
    private AnonymityLevel anonymityLevel;

    private String lastError;
    private String responseData;

    public Proxy toJavaProxy() {
        if (this == NO_PROXY) {
            return Proxy.NO_PROXY;
        }

        return new Proxy(protocol, InetSocketAddress.createUnresolved(ip, port));
    }

    public void addTimesAlive() {
        if (timesAlive == null) {
            timesAlive = 0;
        }
        timesAlive++;
        timesDeadInRow = 0;
    }

    public void addTimesDead() {
        if (timesDead == null) {
            timesDead = 0;
        }
        timesDead++;
        timesDeadInRow++;
    }

    public void updateLatency(int latencyMillis) {
        if (this.latencyMillis == null || this.latencyMillis <= 0) {
            this.latencyMillis = latencyMillis;
        } else {
            this.latencyMillis = (this.latencyMillis + latencyMillis) / 2;
        }
    }
}
