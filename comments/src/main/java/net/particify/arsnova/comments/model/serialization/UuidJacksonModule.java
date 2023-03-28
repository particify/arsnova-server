package net.particify.arsnova.comments.model.serialization;

import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UuidJacksonModule extends SimpleModule {
  private static final Logger logger = LoggerFactory.getLogger(UuidJacksonModule.class);
  public UuidJacksonModule() {
    super("UuidModule");
    this.addSerializer(UUID.class, new StdDelegatingSerializer(new UuidToStringJsonConverter()));
    this.addDeserializer(UUID.class, new StdDelegatingDeserializer<>(new StringToUuidJsonConverter()));
  }

  @Override
  public void setupModule(final SetupContext context) {
    logger.debug("Setting up Jackson UuidModule.");
    super.setupModule(context);
  }
}
