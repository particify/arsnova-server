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
	private String type;

	public User(Google2Profile profile) {
		setUsername(profile.getEmail());
		setType("google");
	}

	public User(TwitterProfile profile) {
		setUsername(profile.getScreenName());
		setType("twitter");
	}

	public User(FacebookProfile profile) {
		setUsername(profile.getLink());
		setType("facebook");
	}

	public User(AttributePrincipal principal) {
		setUsername(principal.getName());
		setType("thm");
	}

	public User(AnonymousAuthenticationToken token) {
		setUsername("anonymous");
		setType("anonymous");
	}

	public User(UsernamePasswordAuthenticationToken token) {
		setUsername(token.getName());
		setType("ldap");
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String toString() {
		return "User, username: " + this.username + ", type: " + this.type;
	}
	@Override
	public int hashCode() {
		return username.concat(type).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		User other = (User) obj;
		return this.username.equals(other.username) && this.type.equals(other.type);
	}

}
