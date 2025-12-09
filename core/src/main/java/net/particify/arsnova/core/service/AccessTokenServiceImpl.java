package net.particify.arsnova.core.service;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.config.ConditionalOnLegacyDataManagement;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.config.properties.TemplateProperties;
import net.particify.arsnova.core.model.AccessToken;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.persistence.AccessTokenRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.security.AuthenticationService;
import net.particify.arsnova.core.security.PasswordUtils;
import net.particify.arsnova.core.security.RoomRole;
import net.particify.arsnova.core.security.User;

@Service
@Primary
@ConditionalOnLegacyDataManagement
public class AccessTokenServiceImpl extends DefaultEntityServiceImpl<AccessToken> implements AccessTokenService {
  private static final Logger logger = LoggerFactory.getLogger(AccessTokenServiceImpl.class);

  private final AccessTokenRepository accessTokenRepository;
  private final PasswordUtils passwordUtils;
  private final EmailService emailService;
  private final AuthenticationService authenticationService;
  private final TemplateProperties templateProperties;
  private final SystemProperties systemProperties;

  public AccessTokenServiceImpl(
      final AccessTokenRepository repository,
      final DeletionRepository deletionRepository,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator,
      final PasswordUtils passwordUtils,
      final EmailService emailService,
      final AuthenticationService authenticationService,
      final TemplateProperties templateProperties,
      final SystemProperties systemProperties) {
    super(AccessToken.class, repository, deletionRepository, jackson2HttpMessageConverter.getObjectMapper(), validator);
    this.accessTokenRepository = repository;
    this.passwordUtils = passwordUtils;
    this.emailService = emailService;
    this.authenticationService = authenticationService;
    this.templateProperties = templateProperties;
    this.systemProperties = systemProperties;
  }

  public AccessToken generate(final String roomId, final RoomRole roomRole) {
    final AccessToken accessToken = new AccessToken();
    accessToken.setRoomId(roomId);
    accessToken.setRole(roomRole);
    accessToken.setToken(passwordUtils.generateKey());
    accessToken.setExpirationDate(LocalDateTime.now().plus(1, ChronoUnit.WEEKS));

    return create(accessToken);
  }

  @Override
  public AccessToken generateAndSendInvite(final Room room, final RoomRole roomRole, final String emailAddress) {
    final AccessToken accessToken = generate(room.getId(), roomRole);
    sendInvite(room, accessToken, emailAddress);
    return accessToken;
  }

  @Override
  public Optional<RoomRole> redeemToken(final String roomId, final String token) {
    final User user = authenticationService.getCurrentUser();
    final Optional<AccessToken> accessToken = accessTokenRepository.findByRoomIdAndToken(roomId, token)
        .filter(t -> t.getUserId() == null || t.getUserId().equals(user.getId()));
    accessToken.ifPresent(t -> {
      t.setUserId(user.getId());
      update(t);
    });

    return accessToken.map(t -> t.getRole());
  }

  private void sendInvite(final Room room, final AccessToken accessToken, final String emailAddress) {
    logger.debug("Sending invitation with token to {}: {}", emailAddress, accessToken);
    final String url = MessageFormat.format(templateProperties.getRoomInvitationUrl(),
        room.getShortId(), accessToken.getToken(), systemProperties.getRootUrl());
    final String subject = MessageFormat.format(templateProperties.getRoomInvitationMailSubject(),
        room.getShortId(), accessToken.getToken(), systemProperties.getRootUrl(), url, room.getName());
    final String body = MessageFormat.format(templateProperties.getRoomInvitationMailBody(),
        room.getShortId(), accessToken.getToken(), systemProperties.getRootUrl(), url, room.getName());
    emailService.sendEmail(emailAddress, subject, body);
  }
}
