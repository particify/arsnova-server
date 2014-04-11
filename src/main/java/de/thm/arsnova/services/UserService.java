package de.thm.arsnova.services;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.scribe.up.profile.facebook.FacebookProfile;
import org.scribe.up.profile.google.Google2Profile;
import org.scribe.up.profile.twitter.TwitterProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
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

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationToken;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class UserService implements IUserService {

	private static final int DEFAULT_SCHEDULER_DELAY_MS = 60000;

	private static final int MAX_USER_INACTIVE_SECONDS = 120;

	public static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	private static final ConcurrentHashMap<UUID, User> socketid2user = new ConcurrentHashMap<UUID, User>();

	/* used for Socket.IO online check solution (new) */
	private static final ConcurrentHashMap<User, String> user2session = new ConcurrentHashMap<User, String>();

	/* used for HTTP polling online check solution (legacy) */
	private static final ConcurrentHashMap<User, String> user2sessionLegacy = new ConcurrentHashMap<User, String>();

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private ARSnovaSocketIOServer socketIoServer;

	@Autowired
	private MailSender mailSender;

	@Autowired
	private SimpleMailMessage regMailTemplate;

	@Value("${security.user-db.allowed-email-domains}")
	private String allowedEmailDomains;

	@Value("${security.arsnova-url}")
	private String arsnovaUrl;

	private Pattern mailPattern;
	private BytesKeyGenerator keygen;
	private BCryptPasswordEncoder encoder;

	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY_MS)
	public final void removeInactiveUsersFromLegacyMap() {
		List<String> usernames = databaseDao.getActiveUsers(MAX_USER_INACTIVE_SECONDS);
		Set<String> affectedSessions = new HashSet<String>();

		for (Entry<User, String> e : user2sessionLegacy.entrySet()) {
			User key = e.getKey();
			if (usernames != null && !usernames.contains(key.getUsername())) {
				if (null != e.getValue()) {
					affectedSessions.add(e.getValue());
				} else {
					LOGGER.warn("Session for user {} is null", key);
				}
				user2sessionLegacy.remove(e.getKey());
			}
		}

		for (String sessionKeyword : affectedSessions) {
			socketIoServer.reportActiveUserCountForSession(sessionKeyword);
		}
	}

	@Override
	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			return null;
		}

		User user = null;

		if (authentication instanceof OAuthAuthenticationToken) {
			OAuthAuthenticationToken token = (OAuthAuthenticationToken) authentication;
			if (token.getUserProfile() instanceof Google2Profile) {
				Google2Profile profile = (Google2Profile) token.getUserProfile();
				user = new User(profile);
			} else if (token.getUserProfile() instanceof TwitterProfile) {
				TwitterProfile profile = (TwitterProfile) token.getUserProfile();
				user = new User(profile);
			} else if (token.getUserProfile() instanceof FacebookProfile) {
				FacebookProfile profile = (FacebookProfile) token.getUserProfile();
				user = new User(profile);
			}
		} else if (authentication instanceof CasAuthenticationToken) {
			CasAuthenticationToken token = (CasAuthenticationToken) authentication;
			user = new User(token.getAssertion().getPrincipal());
		} else if (authentication instanceof AnonymousAuthenticationToken) {
			AnonymousAuthenticationToken token = (AnonymousAuthenticationToken) authentication;
			user = new User(token);
		} else if (authentication instanceof UsernamePasswordAuthenticationToken) {
			UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
			user = new User(token);
			if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"))) {
				user.setType(User.GUEST);
			} else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DB_USER"))) {
				user.setType(User.ARSNOVA);
			}
		}

		if (user == null || user.getUsername().equals("anonymous")) {
			throw new UnauthorizedException();
		}

		return user;
	}

	@Override
	public User getUser2SocketId(UUID socketId) {
		return socketid2user.get(socketId);
	}

	@Override
	public void putUser2SocketId(UUID socketId, User user) {
		socketid2user.put(socketId, user);
	}

	@Override
	public Set<Map.Entry<UUID, User>> socketId2User() {
		return socketid2user.entrySet();
	}

	@Override
	public void removeUser2SocketId(UUID socketId) {
		socketid2user.remove(socketId);
	}

	@Override
	public boolean isUserInSession(User user, String keyword) {
		if (keyword == null) {
			return false;
		}
		String session = user2sessionLegacy.get(user);
		if (session == null) {
			session = user2session.get(user);
			if (session == null) {
				return false;
			}
		}

		return keyword.equals(session);
	}

	@Override
	public Set<User> getUsersInSession(String keyword) {
		Set<User> result = new HashSet<User>();
		for (Entry<User, String> e : user2session.entrySet()) {
			if (e.getValue().equals(keyword)) {
				result.add(e.getKey());
			}
		}
		for (Entry<User, String> e : user2sessionLegacy.entrySet()) {
			if (e.getValue().equals(keyword)) {
				result.add(e.getKey());
			}
		}

		return result;
	}

	@Override
	public int getUsersInSessionCount(String keyword) {
		return getUsersInSession(keyword).size();
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void addCurrentUserToSessionMap(String keyword) {
		User user = getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		user2sessionLegacy.put(user, keyword);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void addUserToSessionBySocketId(UUID socketId, String keyword) {
		User user = socketid2user.get(socketId);
		user2session.put(user, keyword);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void removeUserFromSessionBySocketId(UUID socketId) {
		User user = socketid2user.get(socketId);
		if (null == user) {
			LOGGER.warn("null == user for socket {}", socketId);

			return;
		}
		user2session.remove(user);
	}

	@Override
	public String getSessionForUser(String username) {
		for (Entry<User, String> entry  : user2session.entrySet()) {
			if (entry.getKey().getUsername().equals(username)) {
				return entry.getValue();
			}
		}
		for (Entry<User, String> entry  : user2sessionLegacy.entrySet()) {
			if (entry.getKey().getUsername().equals(username)) {
				return entry.getValue();
			}
		}

		return null;
	}

	@PreDestroy
	public void destroy() {
		LOGGER.error("Destroy UserService");
	}

	@Override
	public void removeUserFromMaps(User user) {
		if (user != null) {
			user2session.remove(user);
			user2sessionLegacy.remove(user);
		}
	}

	@Override
	public int loggedInUsers() {
		return user2sessionLegacy.size();
	}

	@Override
	public DbUser getDbUser(String username) {
		return databaseDao.getUser(username);
	}

	@Override
	public DbUser createDbUser(String username, String password) {
		if (null == keygen) {
			keygen = KeyGenerators.secureRandom(32);
		}

		if (null == mailPattern) {
			parseMailAddressPattern();
		}

		if (null == mailPattern || !mailPattern.matcher(username).matches()) {
			return null;
		}

		if (null != databaseDao.getUser(username)) {
			return null;
		}

		DbUser dbUser = new DbUser();
		dbUser.setUsername(username);
		dbUser.setPassword(encodePassword(password));
		dbUser.setActivationKey(RandomStringUtils.randomAlphanumeric(32));
		dbUser.setCreation(System.currentTimeMillis());

		DbUser result = databaseDao.createOrUpdateUser(dbUser);
		if (null != result) {
			sendActivationEmail(result);
		}

		return result;
	}

	public String encodePassword(String password) {
		if (null == encoder) {
			encoder = new BCryptPasswordEncoder(12);
		}

		return encoder.encode(password);
	}

	public void sendActivationEmail(DbUser dbUser) {
		SimpleMailMessage msg = new SimpleMailMessage(regMailTemplate);
		String activationUrl = MessageFormat.format("{0}/user/activate?username={1}&key={2}", arsnovaUrl, dbUser.getUsername(), dbUser.getActivationKey());
		msg.setTo(dbUser.getUsername());
		msg.setText(MessageFormat.format(msg.getText(), activationUrl));
		LOGGER.debug("Activation mail body: {}", msg.getText());

		try {
			LOGGER.info("Sending activation mail to {}", dbUser.getUsername());
			mailSender.send(msg);
		} catch (MailException e) {
			LOGGER.warn("Activation mail could not be sent: {}", e);
		}
	}

	private void parseMailAddressPattern() {
		/* TODO: Add Unicode support */

		List<String> domainList = Arrays.asList(allowedEmailDomains.split(","));

		if (domainList.size() > 0) {
			List<String> patterns = new ArrayList<String>();
			if (domainList.contains("*")) {
				patterns.add("([a-z0-9-]\\.)+[a-z0-9-]");
			} else {
				Pattern patternPattern = Pattern.compile("[a-z0-9.*-]+", Pattern.CASE_INSENSITIVE);
				for (String patternStr : domainList) {
					if (patternPattern.matcher(patternStr).matches()) {
						patterns.add(patternStr.replaceAll("[.]", "[.]").replaceAll("[*]", "[a-z0-9-]+?"));
					}
				}
			}

			mailPattern = Pattern.compile("[a-z0-9._-]+?@(" + StringUtils.join(patterns, "|") + ")", Pattern.CASE_INSENSITIVE);
			LOGGER.info("Allowed e-mail addresses (pattern) for registration: " + mailPattern.pattern());
		}
	}

	@Override
	public DbUser updateDbUser(DbUser dbUser) {
		if (null != dbUser.getId()) {
			return databaseDao.createOrUpdateUser(dbUser);
		}

		return null;
	}
}
