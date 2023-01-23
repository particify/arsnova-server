package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.model.Announcement;
import net.particify.arsnova.core.persistence.AnnouncementRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.security.AuthenticationService;

@Service
@Primary
public class AnnouncementServiceImpl extends DefaultEntityServiceImpl<Announcement> implements AnnouncementService {
  private AnnouncementRepository announcementRepository;
  private AuthenticationService authenticationService;

  public AnnouncementServiceImpl(
      final AnnouncementRepository repository,
      final DeletionRepository deletionRepository,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator,
      final AuthenticationService authenticationService) {
    super(
        Announcement.class,
        repository,
        deletionRepository,
        jackson2HttpMessageConverter.getObjectMapper(),
        validator);
    this.announcementRepository = repository;
    this.authenticationService = authenticationService;
  }

  @Override
  public List<Announcement> getByRoomId(final String roomId) {
    final List<String> ids = announcementRepository.findIdsByRoomId(roomId);
    return get(ids);
  }

  @Override
  public List<Announcement> getByRoomIds(final List<String> roomIds) {
    final List<String> ids = announcementRepository.findIdsByRoomIds(roomIds);
    return get(ids);
  }

  @Override
  protected void prepareCreate(final Announcement announcement) {
    final String creatorId = authenticationService.getCurrentUser().getId();
    announcement.setCreatorId(creatorId);
  }
}
