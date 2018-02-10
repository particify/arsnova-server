/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.services;

import com.codahale.metrics.annotation.Gauge;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.UserRepository;
import de.thm.arsnova.security.GuestUserDetailsService;
import de.thm.arsnova.security.User;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import org.springframework.web.util.UriUtils;
import org.stagemonitor.core.metrics.MonitorGauges;

import javax.annotation.PreDestroy;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Performs all user related operations.
 */
@Service
@MonitorGauges
public class UserServiceImpl extends DefaultEntityServiceImpl<UserProfile> implements UserService {

	private static final int LOGIN_TRY_RESET_DELAY_MS = 30 * 1000;

	private static final int LOGIN_BAN_RESET_DELAY_MS = 2 * 60 * 1000;

	private static final int REPEATED_PASSWORD_RESET_DELAY_MS = 3 * 60 * 1000;

	private static final int PASSWORD_RESET_KEY_DURABILITY_MS = 2 * 60 * 60 * 1000;

	private static final long ACTIVATION_KEY_CHECK_INTERVAL_MS = 30 * 60 * 1000L;
	private static final long ACTIVATION_KEY_DURABILITY_MS = 6 * 60 * 60 * 1000L;

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	private static final ConcurrentHashMap<UUID, UserAuthentication> socketIdToUser = new ConcurrentHashMap<>();

	/* used for Socket.IO online check solution (new) */
	private static final ConcurrentHashMap<UserAuthentication, String> userToRoomId = new ConcurrentHashMap<>();

	private UserRepository userRepository;

	private JavaMailSender mailSender;

	@Autowired(required = false)
	private GuestUserDetailsService guestUserDetailsService;

	@Autowired(required = false)
	private DaoAuthenticationProvider daoProvider;

	@Autowired(required = false)
	private LdapAuthenticationProvider ldapAuthenticationProvider;

	@Value("${root-url}")
	private String rootUrl;

	@Value("${customization.path}")
	private String customizationPath;

	@Value("${security.user-db.allowed-email-domains}")
	private String allowedEmailDomains;

	@Value("${security.user-db.activation-path}")
	private String activationPath;

	@Value("${security.user-db.reset-password-path}")
	private String resetPasswordPath;

	@Value("${mail.sender.address}")
	private String mailSenderAddress;

	@Value("${mail.sender.name}")
	private String mailSenderName;

	@Value("${security.user-db.registration-mail.subject}")
	private String regMailSubject;

	@Value("${security.user-db.registration-mail.body}")
	private String regMailBody;

	@Value("${security.user-db.reset-password-mail.subject}")
	private String resetPasswordMailSubject;

	@Value("${security.user-db.reset-password-mail.body}")
	private String resetPasswordMailBody;

	@Value("${security.authentication.login-try-limit}")
	private int loginTryLimit;

	@Value("${security.admin-accounts}")
	private String[] adminAccounts;

	private Pattern mailPattern;
	private BytesKeyGenerator keygen;
	private BCryptPasswordEncoder encoder;
	private ConcurrentHashMap<String, Byte> loginTries;
	private Set<String> loginBans;

	{
		loginTries = new ConcurrentHashMap<>();
		loginBans = Collections.synchronizedSet(new HashSet<String>());
	}

	public UserServiceImpl(
			UserRepository repository,
			JavaMailSender mailSender,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(UserProfile.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.userRepository = repository;
		this.mailSender = mailSender;
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

	@Scheduled(fixedDelay = ACTIVATION_KEY_CHECK_INTERVAL_MS)
	public void deleteInactiveUsers() {
		logger.info("Delete inactive users.");
		long unixTime = System.currentTimeMillis();
		long lastActivityBefore = unixTime - ACTIVATION_KEY_DURABILITY_MS;
		userRepository.deleteInactiveUsers(lastActivityBefore);
	}

	@Override
	public UserProfile getCurrentUserProfile() {
		final UserAuthentication authentication = getCurrentUser();
		return getByAuthProviderAndLoginId(authentication.getAuthProvider(), authentication.getUsername());
	}

	@Override
	public UserAuthentication getCurrentUser() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			return null;
		}

		UserAuthentication user = new UserAuthentication(authentication);
		if (user == null || "anonymous".equals(user.getUsername())) {
			throw new UnauthorizedException();
		}
		user.setAdmin(Arrays.asList(adminAccounts).contains(user.getUsername()));

		return user;
	}

	@Override
	public boolean isAdmin(final String username) {
		return Arrays.asList(adminAccounts).contains(username);
	}

	@Override
	public boolean isBannedFromLogin(String addr) {
		return loginBans.contains(addr);
	}

	@Override
	public void increaseFailedLoginCount(String addr) {
		Byte tries = loginTries.get(addr);
		if (null == tries) {
			tries = 0;
		}
		if (tries < loginTryLimit) {
			loginTries.put(addr, ++tries);
			if (loginTryLimit == tries) {
				logger.info("Temporarily banned {} from login.", addr);
				loginBans.add(addr);
			}
		}
	}

	@Override
	public UserAuthentication getUserToSocketId(final UUID socketId) {
		return socketIdToUser.get(socketId);
	}

	@Override
	public void putUserToSocketId(final UUID socketId, final UserAuthentication user) {
		socketIdToUser.put(socketId, user);
	}

	@Override
	public Set<Map.Entry<UUID, UserAuthentication>> getSocketIdToUser() {
		return socketIdToUser.entrySet();
	}

	@Override
	public void removeUserToSocketId(final UUID socketId) {
		socketIdToUser.remove(socketId);
	}

	@Override
	public boolean isUserInRoom(final UserAuthentication user, final String roomShortId) {
		if (roomShortId == null) {
			return false;
		}
		String session = userToRoomId.get(user);

		return session != null && roomShortId.equals(session);
	}

	@Override
	public Set<UserAuthentication> getUsersByRoomShortId(final String roomShortId) {
		final Set<UserAuthentication> result = new HashSet<>();
		for (final Entry<UserAuthentication, String> e : userToRoomId.entrySet()) {
			if (e.getValue().equals(roomShortId)) {
				result.add(e.getKey());
			}
		}

		return result;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void addUserToRoomBySocketId(final UUID socketId, final String roomShortId) {
		final UserAuthentication user = socketIdToUser.get(socketId);
		userToRoomId.put(user, roomShortId);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void removeUserFromRoomBySocketId(final UUID socketId) {
		final UserAuthentication user = socketIdToUser.get(socketId);
		if (null == user) {
			logger.warn("No user exists for socket {}.", socketId);

			return;
		}
		userToRoomId.remove(user);
	}

	@Override
	public String getRoomByUserId(final String userId) {
		for (final Entry<UserAuthentication, String> entry  : userToRoomId.entrySet()) {
			if (entry.getKey().getId().equals(userId)) {
				return entry.getValue();
			}
		}

		return null;
	}

	@PreDestroy
	public void destroy() {
		logger.error("Destroy UserServiceImpl");
	}

	@Override
	public void removeUserFromMaps(final UserAuthentication user) {
		if (user != null) {
			userToRoomId.remove(user);
		}
	}

	@Override
	@Gauge
	public int loggedInUsers() {
		return userToRoomId.size();
	}

	@Override
	public void authenticate(final UsernamePasswordAuthenticationToken token,
			final UserProfile.AuthProvider authProvider) {
		Authentication auth;
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
				UserDetails userDetails = guestUserDetailsService.loadUserByUsername(id, autoCreate);
				if (userDetails == null) {
					throw new UsernameNotFoundException("Guest user does not exist");
				}
				auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

				break;
			default:
				throw new IllegalArgumentException("Unsupported authentication provider");
		}

		if (!auth.isAuthenticated()) {
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
				/* Repository is accessed directly without EntityService to skip permission check */
				userRepository.save(userProfile);
			} else {
				throw new UsernameNotFoundException("User does not exist.");
			}
		}

		return new User(userProfile, grantedAuthorities);
	}

	@Override
	public UserProfile getByAuthProviderAndLoginId(final UserProfile.AuthProvider authProvider, final String loginId) {
		return userRepository.findByAuthProviderAndLoginId(authProvider, loginId);
	}

	@Override
	public UserProfile getByUsername(String username) {
		return userRepository.findByAuthProviderAndLoginId(UserProfile.AuthProvider.ARSNOVA, username.toLowerCase());
	}

	@Override
	public UserProfile create(String username, String password) {
		String lcUsername = username.toLowerCase();

		if (null == keygen) {
			keygen = KeyGenerators.secureRandom(16);
		}

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

		UserProfile userProfile = new UserProfile();
		UserProfile.Account account = new UserProfile.Account();
		userProfile.setAccount(account);
		userProfile.setAuthProvider(UserProfile.AuthProvider.ARSNOVA);
		userProfile.setLoginId(lcUsername);
		account.setPassword(encodePassword(password));
		account.setActivationKey(RandomStringUtils.randomAlphanumeric(32));
		userProfile.setCreationTimestamp(new Date());

		/* Repository is accessed directly without EntityService to skip permission check */
		UserProfile result = userRepository.save(userProfile);
		if (null != result) {
			sendActivationEmail(result);
		} else {
			logger.error("User registration failed. {} could not be created.", lcUsername);
		}

		return result;
	}

	private String encodePassword(String password) {
		if (null == encoder) {
			encoder = new BCryptPasswordEncoder(12);
		}

		return encoder.encode(password);
	}

	private void sendActivationEmail(UserProfile userProfile) {
		String activationUrl;
		try {
			activationUrl = MessageFormat.format(
				"{0}{1}/{2}?action=activate&username={3}&key={4}",
				rootUrl,
				customizationPath,
				activationPath,
				UriUtils.encodeQueryParam(userProfile.getLoginId(), "UTF-8"),
				userProfile.getAccount().getActivationKey()
			);
		} catch (UnsupportedEncodingException e) {
			logger.error("Sending of activation mail failed.", e);

			return;
		}

		sendEmail(userProfile, regMailSubject, MessageFormat.format(regMailBody, activationUrl));
	}

	private void parseMailAddressPattern() {
		/* TODO: Add Unicode support */

		List<String> domainList = Arrays.asList(allowedEmailDomains.split(","));

		if (!domainList.isEmpty()) {
			List<String> patterns = new ArrayList<>();
			if (domainList.contains("*")) {
				patterns.add("([a-z0-9-]+\\.)+[a-z0-9-]+");
			} else {
				Pattern patternPattern = Pattern.compile("[a-z0-9.*-]+", Pattern.CASE_INSENSITIVE);
				for (String patternStr : domainList) {
					if (patternPattern.matcher(patternStr).matches()) {
						patterns.add(patternStr.replaceAll("[.]", "[.]").replaceAll("[*]", "[a-z0-9-]+?"));
					}
				}
			}

			mailPattern = Pattern.compile("[a-z0-9._-]+?@(" + StringUtils.join(patterns, "|") + ")", Pattern.CASE_INSENSITIVE);
			logger.info("Allowed e-mail addresses (pattern) for registration: '{}'.", mailPattern.pattern());
		}
	}

	@Override
	public UserProfile update(UserProfile userProfile) {
		if (null != userProfile.getId()) {
			return userRepository.save(userProfile);
		}

		return null;
	}

	@Override
	public UserProfile deleteByUsername(String username) {
		UserAuthentication user = getCurrentUser();
		if (!user.getUsername().equals(username.toLowerCase())
				&& !SecurityContextHolder.getContext().getAuthentication().getAuthorities()
						.contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
			throw new UnauthorizedException();
		}

		UserProfile userProfile = getByUsername(username);
		if (null == userProfile) {
			throw new NotFoundException();
		}

		userRepository.delete(userProfile);

		return userProfile;
	}

	@Override
	public void initiatePasswordReset(String username) {
		UserProfile userProfile = getByUsername(username);
		if (null == userProfile) {
			logger.info("Password reset failed. User {} does not exist.", username);

			throw new NotFoundException();
		}
		UserProfile.Account account = userProfile.getAccount();
		if (System.currentTimeMillis() < account.getPasswordResetTime().getTime() + REPEATED_PASSWORD_RESET_DELAY_MS) {
			logger.info("Password reset failed. The reset delay for User {} is still active.", username);

			throw new BadRequestException();
		}

		account.setPasswordResetKey(RandomStringUtils.randomAlphanumeric(32));
		account.setPasswordResetTime(new Date());

		if (null == userRepository.save(userProfile)) {
			logger.error("Password reset failed. {} could not be updated.", username);
		}

		String resetPasswordUrl;
		try {
			resetPasswordUrl = MessageFormat.format(
				"{0}{1}/{2}?action=resetpassword&username={3}&key={4}",
				rootUrl,
				customizationPath,
				resetPasswordPath,
				UriUtils.encodeQueryParam(userProfile.getLoginId(), "UTF-8"),
					account.getPasswordResetKey()
			);
		} catch (UnsupportedEncodingException e) {
			logger.error("Sending of password reset mail failed.", e);

			return;
		}

		sendEmail(userProfile, resetPasswordMailSubject, MessageFormat.format(resetPasswordMailBody, resetPasswordUrl));
	}

	@Override
	public boolean resetPassword(UserProfile userProfile, String key, String password) {
		UserProfile.Account account = userProfile.getAccount();
		if (null == key || "".equals(key) || !key.equals(account.getPasswordResetKey())) {
			logger.info("Password reset failed. Invalid key provided for User {}.", userProfile.getLoginId());

			return false;
		}
		if (System.currentTimeMillis() > account.getPasswordResetTime().getTime() + PASSWORD_RESET_KEY_DURABILITY_MS) {
			logger.info("Password reset failed. Key provided for User {} is no longer valid.", userProfile.getLoginId());

			account.setPasswordResetKey(null);
			account.setPasswordResetTime(new Date(0));
			update(userProfile);

			return false;
		}

		account.setPassword(encodePassword(password));
		account.setPasswordResetKey(null);
		if (null == update(userProfile)) {
			logger.error("Password reset failed. {} could not be updated.", userProfile.getLoginId());
		}

		return true;
	}

	private void sendEmail(UserProfile userProfile, String subject, String body) {
		MimeMessage msg = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
		try {
			helper.setFrom(mailSenderName + "<" + mailSenderAddress + ">");
			helper.setTo(userProfile.getLoginId());
			helper.setSubject(subject);
			helper.setText(body);

			logger.info("Sending mail \"{}\" from \"{}\" to \"{}\"", subject, msg.getFrom(), userProfile.getLoginId());
			mailSender.send(msg);
		} catch (MailException | MessagingException e) {
			logger.warn("Mail \"{}\" could not be sent.", subject, e);
		}
	}

	private String generateGuestId() {
		if (null == keygen) {
			keygen = KeyGenerators.secureRandom(16);
		}

		return new String(Hex.encode(keygen.generateKey()));
	}
}
