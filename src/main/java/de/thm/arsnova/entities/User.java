package de.thm.arsnova.entities;

import java.io.Serializable;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.scribe.up.profile.facebook.FacebookProfile;
import org.scribe.up.profile.google.Google2Profile;
import org.scribe.up.profile.twitter.TwitterProfile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class User implements Serializable {
	private static final long serialVersionUID = 1L;
	private String username;

	public User(Google2Profile profile) {
		setUsername(profile.getEmail());
	}

	public User(TwitterProfile profile) {
		setUsername(profile.getScreenName());
	}

	public User(FacebookProfile profile) {
		setUsername(profile.getLink());
	}

	public User(AttributePrincipal principal) {
		setUsername(principal.getName());
	}

	public User(AnonymousAuthenticationToken token) {
		setUsername("anonymous");
	}

	public User(UsernamePasswordAuthenticationToken token) {
		setUsername(token.getName());
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public String toString() {
		return "User, username: " + this.username;
	}
	@Override
	public int hashCode() {
		return username.hashCode();
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || ! obj.getClass().equals(this.getClass())) return false;
		User other = (User) obj;
		return this.username.equals(other.username);
	}

}
