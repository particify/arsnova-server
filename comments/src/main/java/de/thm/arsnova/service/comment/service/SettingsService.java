package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.Settings;
import de.thm.arsnova.service.comment.service.persistence.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private final SettingsRepository repository;

    @Autowired
    public SettingsService(
            final SettingsRepository repository
    ) {
        this.repository = repository;
    }

    public Settings get(String id) {
        Settings defaults = new Settings();
        defaults.setRoomId(id);
        defaults.setDirectSend(true);
        Settings s = repository.findById(id).orElse(defaults);

        return s;
    }

    public Settings create(Settings s) {
        repository.save(s);

        return s;
    }

    public Settings update(final Settings s) {
        return repository.save(s);
    }
}
