package net.particify.arsnova.common.uuid;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.deser.std.StdConvertingDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdDelegatingSerializer;

@Component
public class UuidJacksonModule extends SimpleModule {
  private static final Logger logger = LoggerFactory.getLogger(UuidJacksonModule.class);
  public UuidJacksonModule() {
    super("UuidModule");
    this.addSerializer(UUID.class, new StdDelegatingSerializer(new UuidToStringJsonConverter()));
    this.addDeserializer(UUID.class, new StdConvertingDeserializer<>(new StringToUuidJsonConverter()));
  }

  @Override
  public void setupModule(final SetupContext context) {
    logger.debug("Setting up Jackson UuidModule.");
    super.setupModule(context);
  }
}
