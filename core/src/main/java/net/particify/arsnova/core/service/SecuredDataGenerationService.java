package net.particify.arsnova.core.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SecuredDataGenerationService implements DataGenerationService {
  private final DataGenerationService dataGenerationService;

  public SecuredDataGenerationService(final DataGenerationService dataGenerationService) {
    this.dataGenerationService = dataGenerationService;
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'update')")
  public void generateRandomAnswers(final String roomId) {
    dataGenerationService.generateRandomAnswers(roomId);
  }
}
