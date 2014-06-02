package de.thm.arsnova.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.scribe.up.profile.facebook.FacebookProfile;
import org.scribe.up.profile.google.Google2Profile;
import org.scribe.up.profile.twitter.TwitterProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationToken;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

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

	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY_MS)
	public final void removeInactiveUsersFromLegacyMap() {
		final List<String> usernames = databaseDao.getActiveUsers(MAX_USER_INACTIVE_SECONDS);
		final Set<String> affectedSessions = new HashSet<String>();

		for (final Entry<User, String> e : user2sessionLegacy.entrySet()) {
			final User key = e.getKey();
			if (usernames != null && !usernames.contains(key.getUsername())) {
				if (null != e.getValue()) {
					affectedSessions.add(e.getValue());
				} else {
					LOGGER.warn("Session for user {} is null", key);
				}
				user2sessionLegacy.remove(e.getKey());
			}
		}

		for (final String sessionKeyword : affectedSessions) {
			socketIoServer.reportActiveUserCountForSession(sessionKeyword);
		}
	}

	@Override
	public User getCurrentUser() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			return null;
		}

		User user = null;

		if (authentication instanceof OAuthAuthenticationToken) {
			user = getOAuthUser(authentication, user);
		} else if (authentication instanceof CasAuthenticationToken) {
			final CasAuthenticationToken token = (CasAuthenticationToken) authentication;
			user = new User(token.getAssertion().getPrincipal());
		} else if (authentication instanceof AnonymousAuthenticationToken) {
			final AnonymousAuthenticationToken token = (AnonymousAuthenticationToken) authentication;
			user = new User(token);
		} else if (authentication instanceof UsernamePasswordAuthenticationToken) {
			final UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
			user = new User(token);
		}

		if (user == null || user.getUsername().equals("anonymous")) {
			throw new UnauthorizedException();
		}

		return user;
	}

	private User getOAuthUser(final Authentication authentication, User user) {
		final OAuthAuthenticationToken token = (OAuthAuthenticationToken) authentication;
		if (token.getUserProfile() instanceof Google2Profile) {
			final Google2Profile profile = (Google2Profile) token.getUserProfile();
			user = new User(profile);
		} else if (token.getUserProfile() instanceof TwitterProfile) {
			final TwitterProfile profile = (TwitterProfile) token.getUserProfile();
			user = new User(profile);
		} else if (token.getUserProfile() instanceof FacebookProfile) {
			final FacebookProfile profile = (FacebookProfile) token.getUserProfile();
			user = new User(profile);
		}
		return user;
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
	public Set<User> getUsersInSession(final String keyword) {
		final Set<User> result = new HashSet<User>();
		for (final Entry<User, String> e : user2session.entrySet()) {
			if (e.getValue().equals(keyword)) {
				result.add(e.getKey());
			}
		}
		for (final Entry<User, String> e : user2sessionLegacy.entrySet()) {
			if (e.getValue().equals(keyword)) {
				result.add(e.getKey());
			}
		}

		return result;
	}

	@Override
	public int getUsersInSessionCount(final String keyword) {
		return getUsersInSession(keyword).size();
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void addCurrentUserToSessionMap(final String keyword) {
		final User user = getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		user2sessionLegacy.put(user, keyword);
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
			LOGGER.warn("null == user for socket {}", socketId);

			return;
		}
		user2session.remove(user);
	}

	@Override
	public String getSessionForUser(final String username) {
		for (final Entry<User, String> entry  : user2session.entrySet()) {
			if (entry.getKey().getUsername().equals(username)) {
				return entry.getValue();
			}
		}
		for (final Entry<User, String> entry  : user2sessionLegacy.entrySet()) {
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
	public void removeUserFromMaps(final User user) {
		if (user != null) {
			user2session.remove(user);
			user2sessionLegacy.remove(user);
		}
	}

	@Override
	public int loggedInUsers() {
		return user2sessionLegacy.size();
	}
}
