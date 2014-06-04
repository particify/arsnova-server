package de.thm.arsnova.services;

import java.io.Serializable;
import java.util.UUID;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSessionServiceImpl implements UserSessionService, Serializable {
	private static final long serialVersionUID = 1L;

	private User user;
	private Session session;
	private UUID socketId;
	private Role role;

	@Override
	public void setUser(final User u) {
		user = u;
		user.setRole(role);
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public void setSession(final Session s) {
		session = s;
	}

	@Override
	public Session getSession() {
		return session;
	}

	@Override
	public void setSocketId(final UUID sId) {
		socketId = sId;
	}

	@Override
	public UUID getSocketId() {
		return socketId;
	}

	@Override
	public boolean inSession() {
		return isAuthenticated()
				&& getSession() != null;
	}

	@Override
	public boolean isAuthenticated() {
		return getUser() != null
				&& getRole() != null;
	}

	@Override
	public void setRole(final Role r) {
		role = r;
		if (user != null) {
			user.setRole(role);
		}
	}

	@Override
	public Role getRole() {
		return role;
	}
}
