package net.particify.arsnova.common.uuid;

import java.util.UUID;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.StdConverter;

public class UuidToStringJsonConverter extends StdConverter<UUID, String> {

  @Override
  public String convert(final UUID value) {
    return UuidHelper.uuidToString(value);
  }

  @Override
  public JavaType getInputType(final TypeFactory typeFactory) {
    return typeFactory.constructType(UUID.class);
  }

  @Override
  public JavaType getOutputType(final TypeFactory typeFactory) {
    return typeFactory.constructType(String.class);
  }
}
