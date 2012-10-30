package de.thm.arsnova.entities;

public class Authorize {
	private String user;
	private String socketid;

	public final String getUser() {
		return user;
	}

	public final void setUser(final String user) {
		this.user = user;
	}

	public final String getSocketid() {
		return socketid;
	}

	public final void setSocketid(final String socketid) {
		this.socketid = socketid;
	}

	@Override
	public final String toString() {
		return "user: " + user + ", socketid: " + socketid;

	}
}
