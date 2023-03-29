package net.particify.arsnova.authz.model.serialization

import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UuidJacksonModule : SimpleModule("UuidModule") {
  init {
    this.addSerializer(UUID::class.java, StdDelegatingSerializer(UuidToStringJsonConverter()))
    addDeserializer(UUID::class.java, StdDelegatingDeserializer(StringToUuidJsonConverter()))
  }

  override fun setupModule(context: SetupContext) {
    logger.debug("Setting up Jackson UuidModule.")
    super.setupModule(context)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(UuidJacksonModule::class.java)
  }
}
