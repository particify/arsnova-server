package de.thm.arsnova.services;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.scribe.up.profile.facebook.FacebookProfile;
import org.scribe.up.profile.google.Google2Profile;
import org.scribe.up.profile.twitter.TwitterProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationToken;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;

public class UserService implements IUserService {

	private static final int MAX_USER_INACTIVE_SECONDS = 120;

	public static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	private static final ConcurrentHashMap<UUID, User> socketid2user = new ConcurrentHashMap<UUID, User>();

	/* used for Socket.IO online check solution (new) */
	private static final ConcurrentHashMap<User, String> user2session = new ConcurrentHashMap<User, String>();

	/* used for HTTP polling online check solution (legacy) */
	private static final ConcurrentHashMap<User, String> user2sessionLegacy = new ConcurrentHashMap<User, String>();

	private static final ConcurrentHashMap<User, Date> lastOnline = new ConcurrentHashMap<User, Date>();

	@Override
	public void setLastOnlineActivity(User user, Date date) {
		lastOnline.put(user, date);
		LOGGER.info("Updating active user {}", user.getUsername());
		
		for (Entry<User,Date> e : lastOnline.entrySet()) {
			LOGGER.info("User in Map {}", e.getKey().getUsername());
			if (e.getValue().getTime() < System.currentTimeMillis() - MAX_USER_INACTIVE_SECONDS * 1000) {
				LOGGER.info("Removing inactive user {}", e.getKey());
				lastOnline.remove(e.getKey());
				this.removeUserFromMaps(e.getKey());
				LOGGER.info("Active user count: {}", this.loggedInUsers());
			}
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
			return false;
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
}
