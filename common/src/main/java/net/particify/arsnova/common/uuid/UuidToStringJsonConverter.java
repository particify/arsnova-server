package net.particify.arsnova.common.uuid;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import java.util.UUID;

public class UuidToStringJsonConverter implements Converter<UUID, String> {
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
