package de.thm.arsnova.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import java.util.Collection;

/**
 * Replaces the user ID provided by the authenticating user with the one that is part of LDAP object. This is necessary
 * to get a consistent ID despite case insensitivity.
 */
public class CustomLdapUserDetailsMapper extends LdapUserDetailsMapper {
	public static final Logger LOGGER = LoggerFactory.getLogger(CustomLdapUserDetailsMapper.class);

	private String userIdAttr;

	public CustomLdapUserDetailsMapper(String ldapUserIdAttr) {
		this.userIdAttr = ldapUserIdAttr;
	}

	public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
										  Collection<? extends GrantedAuthority> authorities) {
		String ldapUsername = ctx.getStringAttribute(userIdAttr);
		if (ldapUsername == null) {
			LOGGER.warn("LDAP attribute {} not set. Falling back to lowercased user provided username.", userIdAttr);
			ldapUsername = username.toLowerCase();
		}
		UserDetails userDetails = super.mapUserFromContext(ctx, ldapUsername, authorities);

		return userDetails;
	}
}
