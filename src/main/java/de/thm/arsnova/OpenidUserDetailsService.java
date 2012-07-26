package de.thm.arsnova;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class OpenidUserDetailsService implements UserDetailsService {
    
	public UserDetails loadUserByUsername(String openIdIdentifier) {
		final List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
		grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));
    	
		System.out.println(openIdIdentifier);
		
    	return new User(openIdIdentifier, "", true, true, true, true, grantedAuthorities);
    }
}