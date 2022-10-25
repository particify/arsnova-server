package net.particify.arsnova.core.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.Room;

@Service
public class SecuredDataGenerationService implements DataGenerationService {
  private DataGenerationService dataGenerationService;

  public SecuredDataGenerationService(final DataGenerationService dataGenerationService) {
    this.dataGenerationService = dataGenerationService;
  }

  @Override
  @PreAuthorize("hasPermission(#room, 'update')")
  public void generateRandomAnswers(final Room room) {
    dataGenerationService.generateRandomAnswers(room);
  }
}
