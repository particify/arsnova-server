package net.particify.arsnova.authz.model.serialization

import java.util.UUID
import java.util.regex.Pattern

object UuidHelper {
  private const val REPLACEMENT_PATTERN = "$1-$2-$3-$4-$5"
  private val pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})")

  fun stringToUuid(uuidString: String?): UUID? {
    return if (uuidString.isNullOrEmpty()) {
      null
    } else {
      UUID.fromString(
        pattern.matcher(
          uuidString
        ).replaceFirst(REPLACEMENT_PATTERN)
      )
    }
  }

  @JvmStatic
  fun uuidToString(uuid: UUID?): String? {
    return uuid?.toString()?.replace("-", "")
  }
}
