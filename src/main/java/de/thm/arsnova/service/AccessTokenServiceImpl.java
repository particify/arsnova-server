package de.thm.arsnova.service;

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

import de.thm.arsnova.model.AccessToken;
import de.thm.arsnova.persistence.AccessTokenRepository;
import de.thm.arsnova.security.PasswordUtils;
import de.thm.arsnova.security.RoomRole;
import de.thm.arsnova.security.User;

@Service
@Primary
public class AccessTokenServiceImpl extends DefaultEntityServiceImpl<AccessToken> implements AccessTokenService {
	private static final Logger logger = LoggerFactory.getLogger(AccessTokenServiceImpl.class);

	private final AccessTokenRepository accessTokenRepository;
	private final PasswordUtils passwordUtils;
	private final EmailService emailService;
	private final UserService userService;

	public AccessTokenServiceImpl(
			final AccessTokenRepository repository,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator,
			final PasswordUtils passwordUtils,
			final EmailService emailService,
			final UserService userService) {
		super(AccessToken.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.accessTokenRepository = repository;
		this.passwordUtils = passwordUtils;
		this.emailService = emailService;
		this.userService = userService;
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
	public AccessToken generateAndSendInvite(final String roomId, final RoomRole roomRole, final String emailAddress) {
		final AccessToken accessToken = generate(roomId, roomRole);
		sendInvite(accessToken, emailAddress);
		return accessToken;
	}

	@Override
	public Optional<RoomRole> redeemToken(final String roomId, final String token) {
		final User user = userService.getCurrentUser();
		final Optional<AccessToken> accessToken = accessTokenRepository.findByRoomIdAndToken(roomId, token)
				.filter(t -> t.getUserId() == null || t.getUserId().equals(user.getId()));
		accessToken.ifPresent(t -> {
			t.setUserId(user.getId());
			update(t);
		});

		return accessToken.map(t -> t.getRole());
	}

	private void sendInvite(final AccessToken accessToken, final String emailAddress) {
		logger.debug("Sending invitation with token to {}: {}", emailAddress, accessToken);
		emailService.sendEmail(emailAddress, "INVITE PLACEHOLDER", accessToken.getToken());
	}
}
