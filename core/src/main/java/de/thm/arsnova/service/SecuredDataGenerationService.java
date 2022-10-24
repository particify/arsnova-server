package de.thm.arsnova.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Room;

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
