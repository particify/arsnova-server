package net.particify.arsnova.core.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.Entity;
import net.particify.arsnova.core.model.UserProfile;

@Component
public class ReadOnlyCrudEventListener {
  private final boolean readOnly;

  public ReadOnlyCrudEventListener(final SystemProperties systemProperties) {
    readOnly = systemProperties.isReadOnly();
  }

  @EventListener()
  public void handleBeforeCreationEvent(final BeforeCreationEvent<? extends Entity> event) {
    if (readOnly && !(event.getEntity() instanceof UserProfile)) {
      throw new ReadOnlyException();
    }
  }

  @EventListener()
  public void handleBeforeUpdateEvent(final BeforeUpdateEvent<? extends Entity> event) {
    if (readOnly && !(event.getEntity() instanceof UserProfile)) {
      throw new ReadOnlyException();
    }
  }

  public static class ReadOnlyException extends RuntimeException {}
}
