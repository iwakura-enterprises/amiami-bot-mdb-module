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

    /**
     * Capitalizes the first letter of the input string and makes the rest lowercase.
     *
     * @param name the input string
     *
     * @return the capitalized string
     */
    public static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
