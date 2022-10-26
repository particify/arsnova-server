package de.thm.arsnova.service.comment.service.persistence;

import de.thm.arsnova.service.comment.model.Settings;
import org.springframework.data.repository.CrudRepository;

public interface SettingsRepository extends CrudRepository<Settings, String> {
}
