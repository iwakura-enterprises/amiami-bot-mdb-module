package enterprises.iwakura.amitracker.util;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

@UtilityClass
public class ModalUtils {

    /**
     * Retrieves a string value from a ModalMapping, returning a default value if the mapping is null or blank.
     *
     * @param modalMapping the ModalMapping to retrieve the string from
     * @param defaultValue the default value to return if the mapping is null or blank
     * @return the string value from the ModalMapping, or the default value
     */
    public static String getString(ModalMapping modalMapping, String defaultValue) {
        if (modalMapping == null) {
            return defaultValue;
        }
        var value = modalMapping.getAsString();
        if (value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

}
