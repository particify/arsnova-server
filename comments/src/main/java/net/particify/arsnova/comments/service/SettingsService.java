package net.particify.arsnova.comments.service;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.service.persistence.SettingsRepository;

@Service
public class SettingsService {
  private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
  private final SettingsRepository repository;
  final boolean readOnly;

  @Autowired
  public SettingsService(
      final SettingsRepository repository,
      @Value("${system.read-only:false}") final boolean readOnly
  ) {
    this.repository = repository;
    this.readOnly = readOnly;
  }

  public Settings get(UUID id) {
    Settings defaults = new Settings();
    defaults.setRoomId(id);
    Settings s = repository.findById(id).orElse(defaults);
    if (readOnly) {
      s.setReadonly(true);
    }

    return s;
  }

  public Settings create(Settings s) {
    repository.save(s);

    return s;
  }

  public Settings update(final Settings s) {
    return repository.save(s);
  }

  public void delete(final UUID roomId) {
    Optional<Settings> maybeSettings = repository.findById(roomId);
    maybeSettings.ifPresent(repository::delete);
  }
}
