package de.thm.arsnova.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.scribe.up.profile.facebook.FacebookProfile;
import org.scribe.up.profile.google.Google2Profile;
import org.scribe.up.profile.twitter.TwitterProfile;
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

	private static final ConcurrentHashMap<UUID, User> socketid2user = new ConcurrentHashMap<UUID, User>();
	private static final ConcurrentHashMap<String, String> user2session = new ConcurrentHashMap<String, String>();

	
	@Override
	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			return null;
		}

		User user = null;
		
		if(authentication instanceof OAuthAuthenticationToken) {
			OAuthAuthenticationToken token = (OAuthAuthenticationToken) authentication;
			if(token.getUserProfile() instanceof Google2Profile) {
				Google2Profile profile = (Google2Profile) token.getUserProfile();
				user = new User(profile);
			} else if(token.getUserProfile() instanceof TwitterProfile) {
				TwitterProfile profile = (TwitterProfile) token.getUserProfile();
				user = new User(profile);
			} else if(token.getUserProfile() instanceof FacebookProfile) {
				FacebookProfile profile = (FacebookProfile) token.getUserProfile();
				user = new User(profile);
			}
		} else if (authentication instanceof CasAuthenticationToken) {
			CasAuthenticationToken token = (CasAuthenticationToken) authentication;
			user = new User(token.getAssertion().getPrincipal());
		} else if(authentication instanceof AnonymousAuthenticationToken){
			AnonymousAuthenticationToken token = (AnonymousAuthenticationToken) authentication;
			user = new User(token);
		} else if(authentication instanceof UsernamePasswordAuthenticationToken) {
			UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
			user = new User(token);
		}
		
		if (user == null || user.getUsername().equals("anonymous")) throw new UnauthorizedException();
		
		return user;
	}

	@Override
	public User getUser2SessionID(UUID sessionID) {
		return socketid2user.get(sessionID);
	}

	@Override
	public void putUser2SessionID(UUID sessionID, User user) {
		socketid2user.put(sessionID, user);	
	}

	@Override
	public Set<Map.Entry<UUID, User>> users2Session() {
		return socketid2user.entrySet();
	}

	@Override
	public void removeUser2SessionID(UUID sessionID) {
		socketid2user.remove(sessionID);
	}
	
	@Override
	public boolean isUserInSession(User user, String keyword) {
		if (keyword == null) return false;
		String session = user2session.get(user.getUsername());
		if(session == null) return false;
		return keyword.equals(session);
	}
	
	@Override
	public List<String> getUsersInSession(String keyword) {
		List<String> result = new ArrayList<String>();
		for(Entry<String, String> e : user2session.entrySet()) {
			if(e.getValue().equals(keyword)) {
				result.add(e.getKey());
			}
		}
		return result;
	}	
	
	@Override
	@Transactional(isolation=Isolation.READ_COMMITTED)
	public void addCurrentUserToSessionMap(String keyword) {
		User user = getCurrentUser();
		if (user == null) throw new UnauthorizedException();
		user2session.put(user.getUsername(), keyword);	
	}
	
	@Override
	public String getSessionForUser(String username) {
		return user2session.get(username);
	}
}
