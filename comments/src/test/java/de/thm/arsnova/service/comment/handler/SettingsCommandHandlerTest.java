package de.thm.arsnova.service.comment.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.thm.arsnova.service.comment.model.Settings;
import de.thm.arsnova.service.comment.model.command.CreateSettings;
import de.thm.arsnova.service.comment.model.command.CreateSettingsPayload;
import de.thm.arsnova.service.comment.model.command.UpdateSettings;
import de.thm.arsnova.service.comment.model.command.UpdateSettingsPayload;
import de.thm.arsnova.service.comment.security.PermissionEvaluator;
import de.thm.arsnova.service.comment.service.SettingsService;

@ExtendWith(MockitoExtension.class)
public class SettingsCommandHandlerTest {
  @Mock
  private SettingsService settingsService;

  @Mock
  private PermissionEvaluator permissionEvaluator;

  SettingsCommandHandler commandHandler;

  @BeforeEach
  public void setup() {
    this.commandHandler = new SettingsCommandHandler(settingsService, permissionEvaluator);
  }

  @Test
  public void testCreateSettings() {
    String roomId = "52f08e8314aba247c50faacef600254c";
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
    String roomId = "52f08e8314aba247c50faacef600254c";
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
