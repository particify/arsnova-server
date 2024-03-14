package net.particify.arsnova.common.uuid;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import java.util.UUID;

public class StringToUuidJsonConverter implements Converter<String, UUID> {
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
