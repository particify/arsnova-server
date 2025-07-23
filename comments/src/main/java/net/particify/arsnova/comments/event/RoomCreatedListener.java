package net.particify.arsnova.comments.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.service.SettingsService;

@Component
public class RoomCreatedListener {
  private final SettingsService settingsService;

  public RoomCreatedListener(final SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @EventListener
  @Transactional
  public void handleRoomCreatedEvent(final RoomCreatedEvent event) {
    final Settings settings = new Settings();
    settings.setRoomId(event.getId());
    settings.setDisabled(true);
    settingsService.create(settings);
  }
}
