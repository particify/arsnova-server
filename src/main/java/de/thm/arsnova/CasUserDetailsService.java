package de.thm.arsnova;

import java.util.ArrayList;
import java.util.List;

import org.jasig.cas.client.validation.Assertion;
import org.springframework.security.cas.userdetails.AbstractCasAssertionUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class CasUserDetailsService extends AbstractCasAssertionUserDetailsService {
	
	@Override
	protected UserDetails loadUserDetails(Assertion assertion) {
		final List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
		grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));
		
		System.out.println(assertion.getPrincipal().getName());
		
		return new User(assertion.getPrincipal().getName(), "", true, true, true, true, grantedAuthorities);
	}
}
