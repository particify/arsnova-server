package de.thm.arsnova.config;

import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.scribe.up.provider.impl.FacebookProvider;
import org.scribe.up.provider.impl.Google2Provider;
import org.scribe.up.provider.impl.TwitterProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationProvider;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationEntryPoint;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationFilter;

import de.thm.arsnova.CasUserDetailsService;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig extends SecurityConfig {
	@Override
	protected void configure(HttpSecurity http) {};

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication()
			.withUser("ptsr00")
			.password("secret")
			.authorities("ROLE_USER")
		;
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManager();
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}


	/* Override for test unnecessary Beans with null */

	@Override
	public CasAuthenticationProvider casAuthenticationProvider() {
		return null;
	}

	@Override
	public CasUserDetailsService casUserDetailsService() {
		return null;
	}

	@Override
	public ServiceProperties casServiceProperties() {
		return null;
	}

	@Override
	public Cas20ProxyTicketValidator casTicketValidator() {
		return null;
	}

	@Override
	public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
		return null;
	}

	@Override
	public CasAuthenticationFilter casAuthenticationFilter() {
		return null;
	}

	@Override
	public FacebookProvider facebookProvider() {
		return null;
	}

	@Override
	public OAuthAuthenticationFilter facebookFilter() {
		return null;
	}

	@Override
	public OAuthAuthenticationProvider facebookAuthProvider() {
		return null;
	}

	@Override
	public OAuthAuthenticationEntryPoint facebookEntryPoint() {
		return null;
	}

	@Override
	public Google2Provider googleProvider() {
		return null;
	}

	@Override
	public OAuthAuthenticationFilter googleFilter() {
		return null;
	}

	@Override
	public OAuthAuthenticationProvider googleAuthProvider() {
		return null;
	}

	@Override
	public TwitterProvider twitterProvider() {
		return null;
	}

	@Override
	public OAuthAuthenticationFilter twitterFilter() {
		return null;
	}

	@Override
	public OAuthAuthenticationProvider twitterAuthProvider() {
		return null;
	}
}
