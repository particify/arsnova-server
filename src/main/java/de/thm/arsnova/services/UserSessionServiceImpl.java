package de.thm.arsnova.services;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSessionServiceImpl implements UserSessionService {
	
	private User user;
	private Session session;
	
	@Override
	public void setUser(User u) {
		this.user = u;
	}
	
	@Override
	public User getUser() {
		return user;
	}
	
	@Override
	public void setSession(Session s) {
		this.session = s;
	}
	
	@Override
	public Session getSession() {
		return this.session;
	}
}
