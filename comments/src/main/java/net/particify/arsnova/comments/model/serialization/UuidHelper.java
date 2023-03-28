package net.particify.arsnova.comments.model.serialization;

import java.util.UUID;
import java.util.regex.Pattern;

public class UuidHelper {
  private static final String REPLACEMENT_PATTERN = "$1-$2-$3-$4-$5";
  private static final Pattern pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

  public static UUID stringToUuid(final String uuidString) {
    if (uuidString == null || uuidString.isEmpty()) {
      return null;
    }
    return UUID.fromString(pattern.matcher(uuidString).replaceFirst(REPLACEMENT_PATTERN));
  }

  public static String uuidToString(final UUID uuid) {
    return uuid == null ? null : uuid.toString().replace("-", "");
  }
}
