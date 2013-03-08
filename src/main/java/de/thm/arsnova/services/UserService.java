package de.thm.arsnova.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
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

public class UserService implements IUserService, InitializingBean, DisposableBean {

	public static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	private static final ConcurrentHashMap<UUID, User> socketid2user = new ConcurrentHashMap<UUID, User>();

	/* used for Socket.IO online check solution (new) */
	private static final ConcurrentHashMap<User, String> user2session = new ConcurrentHashMap<User, String>();

	/* used for HTTP polling online check solution (legacy) */
	private static final ConcurrentHashMap<User, String> user2sessionLegacy = new ConcurrentHashMap<User, String>();

	@Autowired
	private IDatabaseDao databaseDao;

	private static final int DEFAULT_SCHEDULER_DELAY_MS = 60000;

	private static final int MAX_USER_INACTIVE_SECONDS = 120;

	@Scheduled(fixedDelay = DEFAULT_SCHEDULER_DELAY_MS)
	public final void removeInactiveUsersFromLegacyMap() {
		List<String> usernames = databaseDao.getInactiveUsers(MAX_USER_INACTIVE_SECONDS);
		LOGGER.info(
			"Inactive users count: {}, user2sessionLegacy count: {}",
			usernames.size(), user2sessionLegacy.size()
		);
		for (Entry<User, String> e : user2sessionLegacy.entrySet()) {
			if (usernames.contains(e.getKey().getUsername())) {
				LOGGER.debug("Removing user {} from user2sessionLegacy", e.getKey());
				user2sessionLegacy.remove(e.getKey());
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
		if (keyword == null)
			return false;
		String session = user2sessionLegacy.get(user);
		if (session == null)
			return false;
		return keyword.equals(session);
	}

	@Override
	public List<User> getUsersInSession(String keyword) {
		List<User> result = new ArrayList<User>();
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
		if (user == null)
			throw new UnauthorizedException();
		LOGGER.info("Mapping user " + user.getUsername() + " to session " + keyword + " [legacy].");
		user2sessionLegacy.put(user, keyword);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void addUserToSessionBySocketId(UUID socketId, String keyword) {
		User user = socketid2user.get(socketId);
		LOGGER.info("Mapping user " + user.getUsername() + " to session " + keyword + ".");
		user2session.put(user, keyword);
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void removeUserFromSessionBySocketId(UUID socketId) {
		User user = socketid2user.get(socketId);
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
	public void afterPropertiesSet() {
		try {
			File tmpDir = new File(System.getProperty("java.io.tmpdir"));
			File store = new File(tmpDir, "arsnova.bin");
			if (!store.exists()) {
				return;
			}
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(store));
			Hashtable<String, Map<?, ?>> map = (Hashtable<String, Map<?, ?>>) ois.readObject();
			ois.close();
			Map<UUID, User> s2u = (Map<UUID, User>) map.get("socketid2user");
			Map<User, String> u2s = (Map<User, String>) map.get("user2session");

			LOGGER.info("load from store: {}", map);

			socketid2user.putAll(s2u);
			user2sessionLegacy.putAll(u2s);

		} catch (IOException e) {
			LOGGER.error("IOException during restoring UserService", e);
		} catch (ClassNotFoundException e) {
			LOGGER.error("ClassNotFoundException during restoring UserService", e);
		}
	}

	@Override
	public void destroy() {
		Hashtable<String, Map<?, ?>> map = new Hashtable<String, Map<?, ?>>();
		map.put("socketid2user", socketid2user);
		map.put("user2session", user2sessionLegacy);

		try {
			File tmpDir = new File(System.getProperty("java.io.tmpdir"));
			File store = new File(tmpDir, "arsnova.bin");
			if (!store.exists()) {
				if (! store.createNewFile()) {
					LOGGER.info("Could not create store file");
				}
			}
			OutputStream file = new FileOutputStream(store);
			ObjectOutputStream objOut = new ObjectOutputStream(file);
			objOut.writeObject(map);
			objOut.close();
			file.close();
			LOGGER.info("saved to store: {}", map);
		} catch (IOException e) {
			LOGGER.error("IOException while saving UserService", e);
		}
	}
}
