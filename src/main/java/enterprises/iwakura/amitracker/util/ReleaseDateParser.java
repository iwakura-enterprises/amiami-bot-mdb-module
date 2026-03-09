package enterprises.iwakura.amitracker.util;

import java.time.LocalDate;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReleaseDateParser {

    public static final String EARLY_STRING = "early";
    public static final String LATE_STRING = "late";

    /**
     * Parses a release date string in the format "MMM-YYYY" (e.g., "Jan-2024") into a LocalDate.
     * If the input is null, blank, or not in the expected format, returns null.
     *
     * @param releaseDate the release date string to parse
     *
     * @return the parsed LocalDate, or null if parsing fails
     */
    public static LocalDate parse(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }

        // Expected format: "MMM-YYYY" (e.g., "Jan-2024")
        String[] parts = releaseDate.split("-");
        if (parts.length == 2) {
            String monthStr = parts[0];
            String yearStr = parts[1];
            int preferredDay = 15; // Default to mid-month

            if (monthStr.contains(" ")) {
                var monthParts = monthStr.split(" ");
                monthStr = monthParts[monthParts.length - 1];

                if (monthParts[0].equalsIgnoreCase(EARLY_STRING)) {
                    preferredDay = 5;
                } else if (monthParts[0].equalsIgnoreCase(LATE_STRING)) {
                    preferredDay = 25;
                }
            }

            try {
                int month = switch (StringUtils.capitalize(monthStr)) {
                    case "Jan" -> 1;
                    case "Feb" -> 2;
                    case "Mar" -> 3;
                    case "Apr" -> 4;
                    case "May" -> 5;
                    case "Jun" -> 6;
                    case "Jul" -> 7;
                    case "Aug" -> 8;
                    case "Sep" -> 9;
                    case "Oct" -> 10;
                    case "Nov" -> 11;
                    case "Dec" -> 12;
                    default -> -1;
                };
                if (month == -1) {
                    return null;
                }
                int year = Integer.parseInt(yearStr);
                return LocalDate.of(year, month, preferredDay); // Set day to 1
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

}
