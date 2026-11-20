package net.particify.arsnova.common.uuid;

import java.util.UUID;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.StdConverter;

public class StringToUuidJsonConverter extends StdConverter<String, UUID> {
  @Override
  public UUID convert(final String value) {
    return UuidHelper.stringToUuid(value);
  }

  @Override
  public JavaType getInputType(final TypeFactory typeFactory) {
    return typeFactory.constructType(String.class);
  }

  @Override
  public JavaType getOutputType(final TypeFactory typeFactory) {
    return typeFactory.constructType(UUID.class);
  }
}
