package enterprises.iwakura.amitracker.console;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import com.sun.management.HotSpotDiagnosticMXBean;

import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.ganyu.annotation.Command;
import enterprises.iwakura.ganyu.annotation.DefaultCommand;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
@RequiredArgsConstructor
@Command("heap-dump")
public class HeapDumpConsoleCommand implements GanyuCommand {

    @DefaultCommand
    public void run() {
        try {
            String filePath = "heap-dump-" + System.currentTimeMillis() + ".hprof";
            HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "com.sun.management:type=HotSpotDiagnostic",
                HotSpotDiagnosticMXBean.class
            );
            mxBean.dumpHeap(filePath, false);
            log.info("Heap dump written to: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to create heap dump", e);
        }
    }
}
