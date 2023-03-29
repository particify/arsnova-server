package net.particify.arsnova.authz.model.serialization

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.Converter
import net.particify.arsnova.authz.model.serialization.UuidHelper.uuidToString
import java.util.UUID

class UuidToStringJsonConverter : Converter<UUID, String?> {
  override fun convert(value: UUID): String? {
    return uuidToString(value)
  }

  override fun getInputType(typeFactory: TypeFactory): JavaType {
    return typeFactory.constructType(UUID::class.java)
  }

  override fun getOutputType(typeFactory: TypeFactory): JavaType {
    return typeFactory.constructType(String::class.java)
  }
}
