/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.model.ClientAuthentication;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.persistence.UserRepository;
import de.thm.arsnova.security.GuestUserDetailsService;
import de.thm.arsnova.security.User;
import de.thm.arsnova.security.jwt.JwtService;
import de.thm.arsnova.security.jwt.JwtToken;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.NotFoundException;

/**
 * Performs all user related operations.
 */
@Service
public class UserServiceImpl extends DefaultEntityServiceImpl<UserProfile> implements UserService {

	private static final int LOGIN_TRY_RESET_DELAY_MS = 30 * 1000;

	private static final int LOGIN_BAN_RESET_DELAY_MS = 2 * 60 * 1000;

	private static final int MAIL_RESEND_TRY_RESET_DELAY_MS = 30 * 1000;

	private static final int MAIL_RESEND_BAN_RESET_DELAY_MS = 2 * 60 * 1000;

	private static final int REPEATED_PASSWORD_RESET_DELAY_MS = 3 * 60 * 1000;

	private static final int PASSWORD_RESET_KEY_DURABILITY_MS = 2 * 60 * 60 * 1000;

	private static final long ACTIVATION_KEY_CHECK_INTERVAL_MS = 30 * 60 * 1000L;
	private static final long ACTIVATION_KEY_DURABILITY_MS = 5 * 24 * 60 * 60 * 1000L;

	private static final int MAX_VERIFICATION_CODE = 999999;
	private static final int MAX_VERIFICATION_CODE_ATTEMPTS = 10;
	private static final String VERIFICATION_CODE_FORMAT = "%06d";

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	private static final ConcurrentHashMap<UUID, String> socketIdToUserId = new ConcurrentHashMap<>();

	/* for the new STOMP over ws functionality */
	private static final ConcurrentHashMap<String, String> wsSessionIdToJwt = new ConcurrentHashMap<>();

	/* used for Socket.IO online check solution (new) */
	private static final ConcurrentHashMap<String, String> userIdToRoomId = new ConcurrentHashMap<>();

	private UserRepository userRepository;
	private JwtService jwtService;
	private JavaMailSender mailSender;

	private SystemProperties systemProperties;
	private SecurityProperties securityProperties;
	private AuthenticationProviderProperties.Registered registeredProperties;

	@Autowired(required = false)
	private GuestUserDetailsService guestUserDetailsService;

	@Autowired(required = false)
	private DaoAuthenticationProvider daoProvider;

	@Autowired(required = false)
	private LdapAuthenticationProvider ldapAuthenticationProvider;

	private String rootUrl;
	private String mailSenderAddress;
	private String mailSenderName;

	@Value("${customization.path}")
	private String customizationPath;

	private Pattern mailPattern;
	private BytesKeyGenerator keygen;
	private SecureRandom secureRandom;
	private BCryptPasswordEncoder encoder;
	private ConcurrentHashMap<String, Byte> loginTries;
	private Set<String> loginBans;
	private ConcurrentHashMap<String, Byte> resentMailCount;
	private Set<String> resendMailBans;

	{
		loginTries = new ConcurrentHashMap<>();
		loginBans = Collections.synchronizedSet(new HashSet<String>());
		resentMailCount = new ConcurrentHashMap<>();
		resendMailBans = Collections.synchronizedSet(new HashSet<String>());
	}

	public UserServiceImpl(
			final UserRepository repository,
			final SystemProperties systemProperties,
			final SecurityProperties securityProperties,
			final AuthenticationProviderProperties authenticationProviderProperties,
			final JavaMailSender mailSender,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator) {
		super(UserProfile.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.userRepository = repository;
		this.securityProperties = securityProperties;
		this.registeredProperties = authenticationProviderProperties.getRegistered();
		this.mailSender = mailSender;
		this.rootUrl = systemProperties.getRootUrl();
		this.mailSenderAddress = systemProperties.getMail().getSenderAddress();
		this.mailSenderName = systemProperties.getMail().getSenderName();
		this.secureRandom = new SecureRandom();
	}

	@Scheduled(fixedDelay = LOGIN_TRY_RESET_DELAY_MS)
	public void resetLoginTries() {
		if (!loginTries.isEmpty()) {
			logger.debug("Reset failed login counters.");
			loginTries.clear();
		}
	}

	@Scheduled(fixedDelay = LOGIN_BAN_RESET_DELAY_MS)
	public void resetLoginBans() {
		if (!loginBans.isEmpty()) {
			logger.info("Reset temporary login bans.");
			loginBans.clear();
		}
	}

	@Scheduled(fixedDelay = MAIL_RESEND_TRY_RESET_DELAY_MS)
	public void resetResendingActivationTries() {
		if (!resentMailCount.isEmpty()) {
			logger.info("Reset failed mail activation resending counters.");
			resentMailCount.clear();
		}
	}

	@Scheduled(fixedDelay = MAIL_RESEND_BAN_RESET_DELAY_MS)
	public void resetResendingActivationBan() {
		if (!resendMailBans.isEmpty()) {
			logger.info("Reset temporary bans from resending activation mail.");
			resendMailBans.clear();
		}
	}

	@Scheduled(fixedDelay = ACTIVATION_KEY_CHECK_INTERVAL_MS)
	public void deleteInactiveUsers() {
		logger.info("Delete inactive users.");
		final long unixTime = System.currentTimeMillis();
		final long lastActivityBefore = unixTime - ACTIVATION_KEY_DURABILITY_MS;
		userRepository.deleteInactiveUsers(lastActivityBefore);
	}

	@Override
	public UserProfile getCurrentUserProfile() {
		final User user = getCurrentUser();
		return getByAuthProviderAndLoginId(user.getAuthProvider(), user.getUsername());
	}

	@Override
	public User getCurrentUser() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
			return null;
		}

		return (User) authentication.getPrincipal();
	}

	@Override
	public de.thm.arsnova.model.ClientAuthentication getCurrentClientAuthentication(final boolean refresh) {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
			return null;
		}
		final User user = (User) authentication.getPrincipal();
		final String jwt = !refresh && authentication instanceof JwtToken
				? (String) authentication.getCredentials() : jwtService.createSignedToken(user, false);

		final ClientAuthentication clientAuthentication =
				new ClientAuthentication(user.getId(), user.getUsername(),
						user.getAuthProvider(), jwt);

		return clientAuthentication;
	}

	@Override
	public boolean isAdmin(final String username) {
		return securityProperties.getAdminAccounts().contains(username);
	}

	@Override
	public boolean isBannedFromLogin(final String addr) {
		return loginBans.contains(addr);
	}

	@Override
	public boolean isBannedFromSendingActivationMail(final String addr) {
		return resendMailBans.contains(addr);
	}

	@Override
	public void increaseFailedLoginCount(final String addr) {
		Byte tries = loginTries.get(addr);
		if (null == tries) {
			tries = 0;
		}
		if (tries < securityProperties.getLoginTryLimit()) {
			loginTries.put(addr, ++tries);
			if (securityProperties.getLoginTryLimit() == tries) {
				logger.info("Temporarily banned {} from login.", addr);
				loginBans.add(addr);
			}
		}
	}

	@Override
	public void increaseSentMailCount(final String addr) {
		Byte tries = resentMailCount.get(addr);
		if (null == tries) {
			tries = 0;
		}
		if (tries < securityProperties.getResendMailLimit()) {
			resentMailCount.put(addr, ++tries);
			if (securityProperties.getResendMailLimit() == tries) {
				logger.info("Temporarily banned {} from resending activation"
						+ " mails in due to too many resent activation mails.", addr);
				resendMailBans.add(addr);
			}
		}
	}

	@Override
	public String getUserIdToSocketId(final UUID socketId) {
		return socketIdToUserId.get(socketId);
	}

	@Override
	public void putUserIdToSocketId(final UUID socketId, final String userId) {
		socketIdToUserId.put(socketId, userId);
	}

	@Override
	public Set<Entry<UUID, String>> getSocketIdToUserId() {
		return socketIdToUserId.entrySet();
	}

	@Override
	public void removeUserToSocketId(final UUID socketId) {
		socketIdToUserId.remove(socketId);
	}

	@Override
	public boolean isUserInRoom(final String userId, final String expectedRoomId) {
		final String actualRoomId = userIdToRoomId.get(userId);

		return actualRoomId != null && actualRoomId.equals(expectedRoomId);
	}

	@Override
	public Set<String> getUsersByRoomId(final String roomId) {
		final Set<String> result = new HashSet<>();
		for (final Entry<String, String> e : userIdToRoomId.entrySet()) {
			if (e.getValue().equals(roomId)) {
				result.add(e.getKey());
			}
		}

		return result;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void addUserToRoomBySocketId(final UUID socketId, final String roomId) {
		final String userId = socketIdToUserId.get(socketId);
		userIdToRoomId.put(userId, roomId);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void removeUserFromRoomBySocketId(final UUID socketId) {
		final String userId = socketIdToUserId.get(socketId);
		if (null == userId) {
			logger.warn("No user exists for socket {}.", socketId);

			return;
		}
		userIdToRoomId.remove(userId);
	}

	@Override
	public String getRoomIdByUserId(final String userId) {
		for (final Entry<String, String> entry  : userIdToRoomId.entrySet()) {
			if (entry.getKey().equals(userId)) {
				return entry.getValue();
			}
		}

		return null;
	}

	@Override
	public void removeUserIdFromMaps(final String userId) {
		if (userId != null) {
			userIdToRoomId.remove(userId);
		}
	}

	@Override
	public int loggedInUsers() {
		return userIdToRoomId.size();
	}

	@Override
	public void authenticate(final UsernamePasswordAuthenticationToken token,
			final UserProfile.AuthProvider authProvider, final String clientAddress) {
		if (isBannedFromLogin(clientAddress)) {
			throw new BadRequestException();
		}

		final Authentication auth;
		switch (authProvider) {
			case LDAP:
				auth = ldapAuthenticationProvider.authenticate(token);
				break;
			case ARSNOVA:
				auth = daoProvider.authenticate(token);
				break;
			case ARSNOVA_GUEST:
				String id = token.getName();
				boolean autoCreate = false;
				if (id == null || id.isEmpty()) {
					id = generateGuestId();
					autoCreate = true;
				}
				final UserDetails userDetails = guestUserDetailsService.loadUserByUsername(id, autoCreate);
				if (userDetails == null) {
					throw new UsernameNotFoundException("Guest user does not exist");
				}
				auth = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());

				break;
			default:
				throw new IllegalArgumentException("Unsupported authentication provider");
		}

		if (!auth.isAuthenticated()) {
			increaseFailedLoginCount(clientAddress);
			throw new BadRequestException();
		}
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Override
	public User loadUser(final UserProfile.AuthProvider authProvider, final String loginId,
			final Collection<GrantedAuthority> grantedAuthorities, final boolean autoCreate)
			throws UsernameNotFoundException {
		logger.debug("Load user: LoginId: {}, AuthProvider: {}", loginId, authProvider);
		UserProfile userProfile = userRepository.findByAuthProviderAndLoginId(authProvider, loginId);
		if (userProfile == null) {
			if (autoCreate) {
				userProfile = new UserProfile(authProvider, loginId);
				final SecurityContext securityContext = SecurityContextHolder.getContext();
				final Authentication oldAuth = securityContext.getAuthentication();
				final Authentication overrideAuth = new AnonymousAuthenticationToken("anonymous", loginId, grantedAuthorities);
				securityContext.setAuthentication(overrideAuth);
				create(userProfile);
				securityContext.setAuthentication(oldAuth);
			} else {
				throw new UsernameNotFoundException("User does not exist.");
			}
		}

		return new User(userProfile, grantedAuthorities);
	}

	@Override
	public User loadUser(final String userId, final Collection<GrantedAuthority> grantedAuthorities)
			throws UsernameNotFoundException {
		logger.debug("Load user: UserId: {}", userId);
		final UserProfile userProfile = get(userId, true);
		if (userProfile == null) {
			throw new UsernameNotFoundException("User does not exist.");
		}

		return new User(userProfile, grantedAuthorities);
	}

	@Override
	public UserProfile getByAuthProviderAndLoginId(final UserProfile.AuthProvider authProvider, final String loginId) {
		return userRepository.findByAuthProviderAndLoginId(authProvider, loginId);
	}

	@Override
	public List<UserProfile> getByLoginId(final String loginId) {
		return userRepository.findByLoginId(loginId);
	}

	@Override
	public UserProfile getByUsername(final String username) {
		return userRepository.findByAuthProviderAndLoginId(UserProfile.AuthProvider.ARSNOVA, username.toLowerCase());
	}

	@Override
	public UserProfile create(final String username, final String password) {
		final String lcUsername = username.toLowerCase();

		if (null == mailPattern) {
			parseMailAddressPattern();
		}

		if (null == mailPattern || !mailPattern.matcher(lcUsername).matches()) {
			logger.info("User registration failed. {} does not match pattern.", lcUsername);

			return null;
		}

		if (null != userRepository.findByAuthProviderAndLoginId(UserProfile.AuthProvider.ARSNOVA, lcUsername)) {
			logger.info("User registration failed. {} already exists.", lcUsername);

			return null;
		}

		final UserProfile userProfile = new UserProfile();
		final UserProfile.Account account = new UserProfile.Account();
		userProfile.setAccount(account);
		userProfile.setAuthProvider(UserProfile.AuthProvider.ARSNOVA);
		userProfile.setLoginId(lcUsername);
		account.setPassword(encodePassword(password));
		account.setActivationKey(generateVerificationCode());
		userProfile.setCreationTimestamp(new Date());

		final UserProfile result = create(userProfile);
		if (null != result) {
			logger.debug("Activation key for user '{}': {}",
					userProfile.getLoginId(),
					account.getActivationKey());
			sendActivationEmail(result);
		} else {
			logger.error("User registration failed. {} could not be created.", lcUsername);
		}

		return result;
	}

	@Override
	public UserProfile createAnonymizedGuestUser() {
		final UserProfile userProfile = new UserProfile(UserProfile.AuthProvider.ANONYMIZED, generateGuestId());
		create(userProfile);
		return userProfile;
	}

	private String encodePassword(final String password) {
		if (null == encoder) {
			encoder = new BCryptPasswordEncoder(12);
		}

		return encoder.encode(password);
	}

	private void sendActivationEmail(final UserProfile userProfile) {
		final String activationKey = userProfile.getAccount().getActivationKey();

		sendEmail(userProfile,
				MessageFormat.format(
						registeredProperties.getRegistrationMailSubject(), activationKey),
				MessageFormat.format(
						registeredProperties.getRegistrationMailBody(), activationKey, rootUrl));
	}

	private void parseMailAddressPattern() {
		/* TODO: Add Unicode support */

		if (!registeredProperties.getAllowedEmailDomains().isEmpty()) {
			final List<String> patterns = new ArrayList<>();
			if (registeredProperties.getAllowedEmailDomains().contains("*")) {
				patterns.add("([a-z0-9-]+\\.)+[a-z0-9-]+");
			} else {
				final Pattern patternPattern = Pattern.compile("[a-z0-9.*-]+", Pattern.CASE_INSENSITIVE);
				for (final String patternStr : registeredProperties.getAllowedEmailDomains()) {
					if (patternPattern.matcher(patternStr).matches()) {
						patterns.add(
								patternStr.replaceAll("[.]", "[.]").replaceAll("[*]", "[a-z0-9-]+?"));
					}
				}
			}

			mailPattern = Pattern.compile("[a-z0-9._-]+?@(" + StringUtils.join(patterns, "|") + ")",
					Pattern.CASE_INSENSITIVE);
			logger.info("Allowed e-mail addresses (pattern) for registration: '{}'.", mailPattern.pattern());
		}
	}

	@Override
	public UserProfile deleteByUsername(final String username) {
		final UserProfile userProfile = getByUsername(username);
		if (null == userProfile) {
			throw new NotFoundException();
		}
		delete(userProfile);

		return userProfile;
	}

	@Override
	public Set<UserProfile.RoomHistoryEntry> getRoomHistory(final UserProfile userProfile) {
		final Set<UserProfile.RoomHistoryEntry> roomHistory = userProfile.getRoomHistory();

		return roomHistory;
	}

	@Override
	@PreAuthorize("hasPermission(#userProfile, 'update')")
	public void addRoomToHistory(final UserProfile userProfile, final Room room) {
		if (userProfile.getId().equals(room.getOwnerId())) {
			return;
		}
		final Set<UserProfile.RoomHistoryEntry> roomHistory = userProfile.getRoomHistory();
		final UserProfile.RoomHistoryEntry entry = new UserProfile.RoomHistoryEntry(room.getId(), new Date());
		/* TODO: lastVisit in roomHistory is currently not updated by subsequent method invocations */
		if (!roomHistory.contains(entry)) {
			roomHistory.add(entry);
			final Map<String, Object> changes = Collections.singletonMap("roomHistory", roomHistory);
			try {
				super.patch(userProfile, changes);
			} catch (final IOException e) {
				logger.error("Could not patch RoomHistory");
			}
		}
	}

	@Override
	@PreAuthorize("hasPermission(#userProfile, 'update')")
	public void deleteRoomFromHistory(final UserProfile userProfile, final Room room) {
		final Set<UserProfile.RoomHistoryEntry> roomHistory = userProfile.getRoomHistory();

		final Set<UserProfile.RoomHistoryEntry> filteredRoomHistory = roomHistory.stream()
				.filter(r -> !r.getRoomId().equals(room.getId()))
				.collect(Collectors.toSet());

		if (filteredRoomHistory.size() < roomHistory.size()) {
			final Map<String, Object> changes = Collections.singletonMap("roomHistory", filteredRoomHistory);
			try {
				super.patch(userProfile, changes);
			} catch (final IOException e) {
				logger.error("Could not patch RoomHistory");
			}
		}
	}

	@Override
	@Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
	public UserProfile resetActivation(final String id, final String clientAddress) {
		if (isBannedFromSendingActivationMail(clientAddress)) {
			return null;
		}
		final UserProfile userProfile = get(id, true);
		if (null == userProfile) {
			logger.info("Reset of account activation failed. User {} does not exist.", id);
			increaseFailedLoginCount(clientAddress);

			throw new NotFoundException();
		}
		sendActivationEmail(userProfile);

		return userProfile;
	}

	@Override
	@Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
	public boolean activateAccount(final String id, final String key, final String clientAddress) {
		if (isBannedFromLogin(clientAddress)) {
			return false;
		}
		final UserProfile userProfile = get(id, true);
		if (userProfile == null || !key.equals(userProfile.getAccount().getActivationKey())) {
			increaseSentMailCount(clientAddress);

			if (userProfile != null) {
				final int failedVerifications = userProfile.getAccount().getFailedVerifications() + 1;
				userProfile.getAccount().setFailedVerifications(failedVerifications);
				if (failedVerifications >= MAX_VERIFICATION_CODE_ATTEMPTS) {
					logger.info("Resetting activation verification code because of too many failed attempts for {}.",
							userProfile.getLoginId());
					userProfile.getAccount().setActivationKey(generateVerificationCode());
					userProfile.getAccount().setFailedVerifications(0);
				}
				update(userProfile);
			}

			return false;
		}

		userProfile.getAccount().setActivationKey(null);
		userProfile.getAccount().setFailedVerifications(0);
		update(userProfile);

		return true;
	}

	@Override
	@Secured("ROLE_ADMIN")
	public void activateAccount(final String id) {
		final UserProfile userProfile = get(id, true);
		userProfile.getAccount().setActivationKey(null);
		userProfile.getAccount().setFailedVerifications(0);
		update(userProfile);
	}

	@Override
	@Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
	public void initiatePasswordReset(final UserProfile userProfile) {
		if (null == userProfile) {
			logger.info("Password reset failed. User does not exist.");

			throw new NotFoundException();
		}
		final UserProfile.Account account = userProfile.getAccount();
		// checks if a password reset process in in progress
		if ((account.getPasswordResetTime() != null)
				&& (System.currentTimeMillis()
						< account.getPasswordResetTime().getTime() + REPEATED_PASSWORD_RESET_DELAY_MS)) {

			logger.info("Password reset failed. The reset delay for User {} is still active.", userProfile.getLoginId());

			throw new BadRequestException();
		}

		account.setPasswordResetKey(generateVerificationCode());
		account.setPasswordResetTime(new Date());
		account.setFailedVerifications(0);
		try {
			update(userProfile);
			logger.debug("Password reset key for user '{}': {}",
					userProfile.getLoginId(),
					account.getPasswordResetKey());
		} catch (final DbAccessException e) {
			logger.error("Password reset failed. {} could not be updated.", userProfile.getLoginId());
			throw e;
		}

		sendEmail(userProfile,
				MessageFormat.format(
						registeredProperties.getResetPasswordMailSubject(), account.getPasswordResetKey()),
				MessageFormat.format(
						registeredProperties.getResetPasswordMailBody(), account.getPasswordResetKey(), rootUrl));
	}

	@Override
	@Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
	public boolean resetPassword(final UserProfile userProfile, final String key, final String password) {
		final UserProfile.Account account = userProfile.getAccount();
		if (null == key || "".equals(key) || !key.equals(account.getPasswordResetKey())) {
			logger.info("Password reset failed. Invalid key provided for User {}.", userProfile.getLoginId());

			if (key != null && !key.equals(account.getPasswordResetKey())) {
				final int failedVerifications = userProfile.getAccount().getFailedVerifications() + 1;
				userProfile.getAccount().setFailedVerifications(failedVerifications);
				if (failedVerifications >= MAX_VERIFICATION_CODE_ATTEMPTS) {
					logger.info(
							"Resetting password reset verification code because of too many failed attempts for {}.",
							userProfile.getLoginId());
					userProfile.getAccount().setPasswordResetKey(null);
					account.setPasswordResetTime(new Date(0));
					account.setFailedVerifications(0);
				}
				update(userProfile);
			}

			return false;
		}
		if (System.currentTimeMillis() > account.getPasswordResetTime().getTime() + PASSWORD_RESET_KEY_DURABILITY_MS) {
			logger.info("Password reset failed. Key provided for User {} is no longer valid.", userProfile.getLoginId());

			account.setPasswordResetKey(null);
			account.setPasswordResetTime(new Date(0));
			account.setFailedVerifications(0);
			update(userProfile);

			return false;
		}

		account.setPassword(encodePassword(password));
		account.setPasswordResetKey(null);
		try {
			update(userProfile);
		} catch (final DbAccessException e) {
			logger.error("Password reset failed. {} could not be updated.", userProfile.getLoginId());
			throw e;
		}

		return true;
	}

	private void sendEmail(final UserProfile userProfile, final String subject, final String body) {
		final MimeMessage msg = mailSender.createMimeMessage();
		final MimeMessageHelper helper = new MimeMessageHelper(msg, CharEncoding.UTF_8);
		try {
			helper.setFrom(mailSenderName + "<" + mailSenderAddress + ">");
			helper.setTo(userProfile.getLoginId());
			helper.setSubject(subject);
			helper.setText(body);

			logger.info("Sending mail \"{}\" from \"{}\" to \"{}\"", subject, msg.getFrom(), userProfile.getLoginId());
			mailSender.send(msg);
		} catch (final MailException e) {
			logger.warn("Mail \"{}\" could not be sent.", subject, e);
			throw e;
		} catch (final MessagingException e) {
			logger.warn("Mail \"{}\" could not be sent because of MessagingException.", subject, e);
		}
	}

	private String generateGuestId() {
		if (null == keygen) {
			keygen = KeyGenerators.secureRandom(16);
		}

		return new String(Hex.encode(keygen.generateKey()));
	}

	private String generateVerificationCode() {
		final int code = secureRandom.nextInt(MAX_VERIFICATION_CODE);
		return String.format(VERIFICATION_CODE_FORMAT, code);
	}

	@Autowired
	public void setJwtService(final JwtService jwtService) {
		this.jwtService = jwtService;
	}

	public void addWsSessionToJwtMapping(final String wsSessionId, final String jwt) {
		wsSessionIdToJwt.put(wsSessionId, jwt);
	}

	public User getAuthenticatedUserByWsSession(final String wsSessionId) {
		final String jwt = wsSessionIdToJwt.getOrDefault(wsSessionId, null);
		if (jwt == null) {
			return null;
		}
		final User u = jwtService.verifyToken(jwt);
		if (u == null) {
			return null;
		}

		return u;
	}
}
