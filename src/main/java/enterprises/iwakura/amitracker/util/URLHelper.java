package enterprises.iwakura.amitracker.util;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

/**
 * Utility class for URL-related helper methods.
 */
@UtilityClass
public class URLHelper {

    private final Pattern AMIAMI_URL_PATTERN = Pattern.compile("https?://(?:www\\.)?amiami\\.com/[^\\s]*[?&](?:gcode|scode)=([A-Za-z0-9\\-]+)");

    /**
     * Finds the first AmiAmi product URL in a string and extracts its product code.
     *
     * @param text                 the text to search for an AmiAmi URL
     * @param returnTextIfNotFound Return text if code not found
     *
     * @return the extracted product code, or null if no AmiAmi URL is found
     */
    public static String extractProductCode(String text, boolean returnTextIfNotFound) {
        if (text == null) {
            return null;
        }
        var matcher = AMIAMI_URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (returnTextIfNotFound) {
            return text;
        } else {
            return null;
        }
    }
}
