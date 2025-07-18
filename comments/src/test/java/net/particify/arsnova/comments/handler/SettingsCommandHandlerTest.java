package net.particify.arsnova.comments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.model.command.CreateSettings;
import net.particify.arsnova.comments.model.command.CreateSettingsPayload;
import net.particify.arsnova.comments.model.command.UpdateSettings;
import net.particify.arsnova.comments.model.command.UpdateSettingsPayload;
import net.particify.arsnova.comments.security.PermissionEvaluator;
import net.particify.arsnova.comments.service.SettingsService;

@ExtendWith(MockitoExtension.class)
public class SettingsCommandHandlerTest {
  @Mock
  private SettingsService settingsService;

  @Mock
  private PermissionEvaluator permissionEvaluator;

  @Mock
  private AmqpTemplate messagingTemplate;

  SettingsCommandHandler commandHandler;

  @BeforeEach
  public void setup() {
    this.commandHandler = new SettingsCommandHandler(messagingTemplate, settingsService, permissionEvaluator, false);
  }

  @Test
  public void testCreateSettings() {
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    CreateSettingsPayload payload = new CreateSettingsPayload();
    payload.setRoomId(roomId);
    payload.setDirectSend(true);
    CreateSettings command = new CreateSettings(payload);

    Settings expectedSettings = new Settings();
    expectedSettings.setRoomId(roomId);
    expectedSettings.setDirectSend(true);

    when(settingsService.create(any())).thenReturn(expectedSettings);
    when(permissionEvaluator.isOwnerOrAnyTypeOfModeratorForRoom(any())).thenReturn(true);

    Settings savedSettings = commandHandler.handle(roomId, command);

    assertThat(savedSettings).isEqualTo(expectedSettings);
  }

  @Test
  public void testUpdateSettings() {
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UpdateSettingsPayload payload = new UpdateSettingsPayload();
    payload.setRoomId(roomId);
    payload.setDirectSend(true);
    UpdateSettings command = new UpdateSettings(payload);

    Settings expectedSettings = new Settings();
    expectedSettings.setRoomId(roomId);
    expectedSettings.setDirectSend(true);

    when(settingsService.get(roomId)).thenReturn(expectedSettings);
    when(settingsService.update(any())).thenReturn(expectedSettings);
    when(permissionEvaluator.isOwnerOrAnyTypeOfModeratorForRoom(any())).thenReturn(true);

    Settings savedSettings = commandHandler.handle(roomId, command);

    assertThat(savedSettings).isEqualTo(expectedSettings);
  }
}
