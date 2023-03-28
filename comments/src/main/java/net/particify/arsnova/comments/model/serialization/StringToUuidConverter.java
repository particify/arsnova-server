package net.particify.arsnova.comments.model.serialization;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToUuidConverter implements Converter<String, UUID> {
  @Override
  public UUID convert(final String value) {
    return UuidHelper.stringToUuid(value);
  }
}
