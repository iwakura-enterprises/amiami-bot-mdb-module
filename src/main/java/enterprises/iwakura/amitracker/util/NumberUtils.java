package enterprises.iwakura.amitracker.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NumberUtils {

    /**
     * Safely parses a String to a Double.
     * Returns null if the input is not a valid Double.
     *
     * @param input the input string
     * @return the parsed Double or null if invalid
     */
    public Double parseSafe(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
