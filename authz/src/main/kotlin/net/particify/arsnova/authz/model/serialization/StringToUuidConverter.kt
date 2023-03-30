package net.particify.arsnova.authz.model.serialization

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StringToUuidConverter : Converter<String, UUID?> {
  override fun convert(value: String): UUID? {
    return UuidHelper.stringToUuid(value)
  }
}
