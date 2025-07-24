package net.particify.arsnova.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.event.BeforeDeletionEvent;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.RoomSettings;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.persistence.RoomSettingsRepository;

@Service
@Primary
public class RoomSettingsServiceImpl
    extends DefaultEntityServiceImpl<RoomSettings>
    implements RoomSettingsService {
  private RoomSettingsRepository roomSettingsRepository;

  public RoomSettingsServiceImpl(
      final RoomSettingsRepository repository,
      final DeletionRepository deletionRepository,
      final ObjectMapper objectMapper,
      final Validator validator) {
    super(RoomSettings.class, repository, deletionRepository, objectMapper, validator);
    roomSettingsRepository = repository;
  }

  @Override
  protected void prepareCreate(final RoomSettings entity) {
    if (entity.getRoomId() == null) {
      throw new IllegalArgumentException("No roomId set.");
    }
    if (getByRoomId(entity.getRoomId()) != null) {
      throw new IllegalStateException("Settings already exist.");
    }
    super.prepareCreate(entity);
  }

  @Override
  public RoomSettings getByRoomId(final String roomId) {
    return roomSettingsRepository.findByRoomId(roomId);
  }

  @EventListener
  public void handleRoomDeletion(final BeforeDeletionEvent<Room> event) {
    final RoomSettings settings = getByRoomId(event.getEntity().getId());
    if (settings != null) {
      delete(settings);
    }
  }
}
