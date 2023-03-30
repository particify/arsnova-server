package net.particify.arsnova.authz.model.serialization

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.Converter
import java.util.UUID

class StringToUuidJsonConverter : Converter<String, UUID?> {
  override fun convert(value: String): UUID? {
    return UuidHelper.stringToUuid(value)
  }

  override fun getInputType(typeFactory: TypeFactory): JavaType {
    return typeFactory.constructType(String::class.java)
  }

  override fun getOutputType(typeFactory: TypeFactory): JavaType {
    return typeFactory.constructType(UUID::class.java)
  }
}
