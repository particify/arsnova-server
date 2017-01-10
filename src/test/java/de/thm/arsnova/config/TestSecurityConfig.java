/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.config;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationProvider;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationEntryPoint;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationFilter;
import de.thm.arsnova.CasUserDetailsService;
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

	@Override
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
