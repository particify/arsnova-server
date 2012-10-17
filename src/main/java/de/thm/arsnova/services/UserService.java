package de.thm.arsnova.services;

import org.scribe.up.profile.facebook.FacebookProfile;
import org.scribe.up.profile.google.Google2Profile;
import org.scribe.up.profile.twitter.TwitterProfile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationToken;

import de.thm.arsnova.entities.User;

public class UserService implements IUserService {

	@Override
	public User getUser(Authentication authentication) {
		if (authentication == null || authentication.getPrincipal() == null) {
			return null;
		}

		if(authentication instanceof OAuthAuthenticationToken) {
			OAuthAuthenticationToken token = (OAuthAuthenticationToken) authentication;
			if(token.getUserProfile() instanceof Google2Profile) {
				Google2Profile profile = (Google2Profile) token.getUserProfile();
				return new User(profile);
			} else if(token.getUserProfile() instanceof TwitterProfile) {
				TwitterProfile profile = (TwitterProfile) token.getUserProfile();
				return new User(profile);
			} else if(token.getUserProfile() instanceof FacebookProfile) {
				FacebookProfile profile = (FacebookProfile) token.getUserProfile();
				return new User(profile);
			}
		} else if (authentication instanceof CasAuthenticationToken) {
			CasAuthenticationToken token = (CasAuthenticationToken) authentication;
			return new User(token.getAssertion().getPrincipal());
		} else if(authentication instanceof AnonymousAuthenticationToken){
			AnonymousAuthenticationToken token = (AnonymousAuthenticationToken) authentication;
			return new User(token);
		} else if(authentication instanceof UsernamePasswordAuthenticationToken) {
			UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
			return new User(token);
		}
		return null;
	}

}
