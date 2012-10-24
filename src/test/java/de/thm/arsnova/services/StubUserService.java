package de.thm.arsnova.services;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import de.thm.arsnova.entities.User;

public class StubUserService implements IUserService {

	private User stubUser = null;
	
	public void setUserAuthenticated(boolean isAuthenticated) {
		if (isAuthenticated) {
			stubUser = new User(new UsernamePasswordAuthenticationToken("ptsr00","testpassword"));
			return;
		}
		stubUser = null;
	}
	
	public void useAnonymousUser() {
		stubUser = new User(new UsernamePasswordAuthenticationToken("anonymous",""));
	}
	
	@Override
	public User getCurrentUser() {
		return stubUser;
	}

	@Override
	public User getUser2SessionID(UUID sessionID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putUser2SessionID(UUID sessionID, User user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeUser2SessionID(UUID sessionID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Entry<UUID, User>> users2Session() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUserInSession(User user, String keyword) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> getUsersInSession(String keyword) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSessionForUser(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCurrentUserToSessionMap(String keyword) {
		// TODO Auto-generated method stub
		
	}
}
