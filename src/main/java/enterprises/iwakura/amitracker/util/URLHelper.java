package enterprises.iwakura.amitracker.util;

import lombok.experimental.UtilityClass;

/**
 * Utility class for URL-related helper methods.
 */
@UtilityClass
public class URLHelper {

    /**
     * Extracts the product code from an AmiAmi product URL.
     *
     * @param amiamiUrl the AmiAmi product URL
     * @return the extracted product code, or null if not found
     */
    public static String extractProductCode(String amiamiUrl) {
        // e.g.
        // https://www.amiami.com/eng/detail?gcode=GOODS-12345678
        // https://www.amiami.com/eng/detail?scode=GOODS-12345678

        if (amiamiUrl != null) {
            var parts = amiamiUrl.split("[?&]");
            for (var part : parts) {
                if (part.startsWith("gcode=") || part.startsWith("scode=")) {
                    return part.split("=")[1];
                }
            }
        }
        return amiamiUrl; // Fallback to returning the parameter itself
    }
}
