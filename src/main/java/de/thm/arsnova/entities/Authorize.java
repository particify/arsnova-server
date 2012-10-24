package de.thm.arsnova.entities;

public class Authorize {
	private String user;
	private String socketid;
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getSocketid() {
		return socketid;
	}
	public void setSocketid(String socketid) {
		this.socketid = socketid;
	}
	
	@Override
	public String toString() {
		return "user: " + user + ", socketid: " + socketid;
		
	}
}
