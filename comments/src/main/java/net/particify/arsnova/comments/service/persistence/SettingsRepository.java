package net.particify.arsnova.comments.service.persistence;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

import net.particify.arsnova.comments.model.Settings;

public interface SettingsRepository extends CrudRepository<Settings, UUID> {
}
