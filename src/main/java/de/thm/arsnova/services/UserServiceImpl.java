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
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.UserRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;
import org.pac4j.oauth.profile.twitter.TwitterProfile;
import org.pac4j.springframework.security.authentication.Pac4jAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Performs all user related operations.
 */
@Service
@MonitorGauges
public class UserServiceImpl implements UserService {

	private static final int LOGIN_TRY_RESET_DELAY_MS = 30 * 1000;

	private static final int LOGIN_BAN_RESET_DELAY_MS = 2 * 60 * 1000;

	private static final int REPEATED_PASSWORD_RESET_DELAY_MS = 3 * 60 * 1000;

	private static final int PASSWORD_RESET_KEY_DURABILITY_MS = 2 * 60 * 60 * 1000;

	private static final long ACTIVATION_KEY_CHECK_INTERVAL_MS = 30 * 60 * 1000L;
	private static final long ACTIVATION_KEY_DURABILITY_MS = 6 * 60 * 60 * 1000L;

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	private static final ConcurrentHashMap<UUID, User> socketid2user = new ConcurrentHashMap<>();

	/* used for Socket.IO online check solution (new) */
	private static final ConcurrentHashMap<User, String> user2session = new ConcurrentHashMap<>();

	private UserRepository userRepository;

	private JavaMailSender mailSender;

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

	public UserServiceImpl(UserRepository repository, JavaMailSender mailSender) {
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
	public User getCurrentUser() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			return null;
		}

		User user = null;

		if (authentication instanceof Pac4jAuthenticationToken) {
			user = getOAuthUser(authentication);
		} else if (authentication instanceof CasAuthenticationToken) {
			final CasAuthenticationToken token = (CasAuthenticationToken) authentication;
			user = new User(token.getAssertion().getPrincipal());
		} else if (authentication instanceof AnonymousAuthenticationToken) {
			final AnonymousAuthenticationToken token = (AnonymousAuthenticationToken) authentication;
			user = new User(token);
		} else if (authentication instanceof UsernamePasswordAuthenticationToken) {
			final UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
			user = new User(token);
			if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"))) {
				user.setType(User.GUEST);
			} else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DB_USER"))) {
				user.setType(User.ARSNOVA);
			}
		}

		if (user == null || "anonymous".equals(user.getUsername())) {
			throw new UnauthorizedException();
		}

		user.setAdmin(Arrays.asList(adminAccounts).contains(user.getUsername()));

		return user;
	}

	private User getOAuthUser(final Authentication authentication) {
		User user = null;
		final Pac4jAuthenticationToken token = (Pac4jAuthenticationToken) authentication;
		if (token.getProfile() instanceof Google2Profile) {
			final Google2Profile profile = (Google2Profile) token.getProfile();
			user = new User(profile);
		} else if (token.getProfile() instanceof TwitterProfile) {
			final TwitterProfile profile = (TwitterProfile) token.getProfile();
			user = new User(profile);
		} else if (token.getProfile() instanceof FacebookProfile) {
			final FacebookProfile profile = (FacebookProfile) token.getProfile();
			user = new User(profile);
		}
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
	public User getUser2SocketId(final UUID socketId) {
		return socketid2user.get(socketId);
	}

	@Override
	public void putUser2SocketId(final UUID socketId, final User user) {
		socketid2user.put(socketId, user);
	}

	@Override
	public Set<Map.Entry<UUID, User>> socketId2User() {
		return socketid2user.entrySet();
	}

	@Override
	public void removeUser2SocketId(final UUID socketId) {
		socketid2user.remove(socketId);
	}

	@Override
	public boolean isUserInSession(final User user, final String keyword) {
		if (keyword == null) {
			return false;
		}
		String session = user2session.get(user);

		return session != null && keyword.equals(session);
	}

	@Override
	public Set<User> getUsersBySessionKey(final String keyword) {
		final Set<User> result = new HashSet<>();
		for (final Entry<User, String> e : user2session.entrySet()) {
			if (e.getValue().equals(keyword)) {
				result.add(e.getKey());
			}
		}

		return result;
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void addUserToSessionBySocketId(final UUID socketId, final String keyword) {
		final User user = socketid2user.get(socketId);
		user2session.put(user, keyword);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void removeUserFromSessionBySocketId(final UUID socketId) {
		final User user = socketid2user.get(socketId);
		if (null == user) {
			logger.warn("No user exists for socket {}.", socketId);

			return;
		}
		user2session.remove(user);
	}

	@Override
	public String getSessionByUsername(final String username) {
		for (final Entry<User, String> entry  : user2session.entrySet()) {
			if (entry.getKey().getUsername().equals(username)) {
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
	public void removeUserFromMaps(final User user) {
		if (user != null) {
			user2session.remove(user);
		}
	}

	@Override
	@Gauge
	public int loggedInUsers() {
		return user2session.size();
	}

	@Override
	public DbUser getByUsername(String username) {
		return userRepository.findByUsername(username.toLowerCase());
	}

	@Override
	public DbUser create(String username, String password) {
		String lcUsername = username.toLowerCase();

		if (null == keygen) {
			keygen = KeyGenerators.secureRandom(32);
		}

		if (null == mailPattern) {
			parseMailAddressPattern();
		}

		if (null == mailPattern || !mailPattern.matcher(lcUsername).matches()) {
			logger.info("User registration failed. {} does not match pattern.", lcUsername);

			return null;
		}

		if (null != userRepository.findByUsername(lcUsername)) {
			logger.info("User registration failed. {} already exists.", lcUsername);

			return null;
		}

		DbUser dbUser = new DbUser();
		dbUser.setUsername(lcUsername);
		dbUser.setPassword(encodePassword(password));
		dbUser.setActivationKey(RandomStringUtils.randomAlphanumeric(32));
		dbUser.setCreation(System.currentTimeMillis());

		DbUser result = userRepository.save(dbUser);
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

	private void sendActivationEmail(DbUser dbUser) {
		String activationUrl;
		try {
			activationUrl = MessageFormat.format(
				"{0}{1}/{2}?action=activate&username={3}&key={4}",
				rootUrl,
				customizationPath,
				activationPath,
				UriUtils.encodeQueryParam(dbUser.getUsername(), "UTF-8"),
				dbUser.getActivationKey()
			);
		} catch (UnsupportedEncodingException e) {
			logger.error("Sending of activation mail failed.", e);

			return;
		}

		sendEmail(dbUser, regMailSubject, MessageFormat.format(regMailBody, activationUrl));
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
	public DbUser update(DbUser dbUser) {
		if (null != dbUser.getId()) {
			return userRepository.save(dbUser);
		}

		return null;
	}

	@Override
	public DbUser deleteByUsername(String username) {
		User user = getCurrentUser();
		if (!user.getUsername().equals(username.toLowerCase())
				&& !SecurityContextHolder.getContext().getAuthentication().getAuthorities()
						.contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
			throw new UnauthorizedException();
		}

		DbUser dbUser = getByUsername(username);
		if (null == dbUser) {
			throw new NotFoundException();
		}

		userRepository.delete(dbUser);

		return dbUser;
	}

	@Override
	public void initiatePasswordReset(String username) {
		DbUser dbUser = getByUsername(username);
		if (null == dbUser) {
			logger.info("Password reset failed. User {} does not exist.", username);

			throw new NotFoundException();
		}
		if (System.currentTimeMillis() < dbUser.getPasswordResetTime() + REPEATED_PASSWORD_RESET_DELAY_MS) {
			logger.info("Password reset failed. The reset delay for User {} is still active.", username);

			throw new BadRequestException();
		}

		dbUser.setPasswordResetKey(RandomStringUtils.randomAlphanumeric(32));
		dbUser.setPasswordResetTime(System.currentTimeMillis());

		if (null == userRepository.save(dbUser)) {
			logger.error("Password reset failed. {} could not be updated.", username);
		}

		String resetPasswordUrl;
		try {
			resetPasswordUrl = MessageFormat.format(
				"{0}{1}/{2}?action=resetpassword&username={3}&key={4}",
				rootUrl,
				customizationPath,
				resetPasswordPath,
				UriUtils.encodeQueryParam(dbUser.getUsername(), "UTF-8"),
				dbUser.getPasswordResetKey()
			);
		} catch (UnsupportedEncodingException e) {
			logger.error("Sending of password reset mail failed.", e);

			return;
		}

		sendEmail(dbUser, resetPasswordMailSubject, MessageFormat.format(resetPasswordMailBody, resetPasswordUrl));
	}

	@Override
	public boolean resetPassword(DbUser dbUser, String key, String password) {
		if (null == key || "".equals(key) || !key.equals(dbUser.getPasswordResetKey())) {
			logger.info("Password reset failed. Invalid key provided for User {}.", dbUser.getUsername());

			return false;
		}
		if (System.currentTimeMillis() > dbUser.getPasswordResetTime() + PASSWORD_RESET_KEY_DURABILITY_MS) {
			logger.info("Password reset failed. Key provided for User {} is no longer valid.", dbUser.getUsername());

			dbUser.setPasswordResetKey(null);
			dbUser.setPasswordResetTime(0);
			update(dbUser);

			return false;
		}

		dbUser.setPassword(encodePassword(password));
		dbUser.setPasswordResetKey(null);
		if (null == update(dbUser)) {
			logger.error("Password reset failed. {} could not be updated.", dbUser.getUsername());
		}

		return true;
	}

	private void sendEmail(DbUser dbUser, String subject, String body) {
		MimeMessage msg = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
		try {
			helper.setFrom(mailSenderName + "<" + mailSenderAddress + ">");
			helper.setTo(dbUser.getUsername());
			helper.setSubject(subject);
			helper.setText(body);

			logger.info("Sending mail \"{}\" from \"{}\" to \"{}\"", subject, msg.getFrom(), dbUser.getUsername());
			mailSender.send(msg);
		} catch (MailException | MessagingException e) {
			logger.warn("Mail \"{}\" could not be sent.", subject, e);
		}
	}
}
