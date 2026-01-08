package enterprises.iwakura.amitracker.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {

    /**
     * Truncates the input string to the specified maximum length.
     *
     * @param input     the input string
     * @param maxLength the maximum length
     *
     * @return the truncated string if it exceeds maxLength, otherwise the original string
     */
    public String maxLength(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }
}
