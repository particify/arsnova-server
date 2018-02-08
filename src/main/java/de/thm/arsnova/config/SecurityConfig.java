/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import de.thm.arsnova.security.CasLogoutSuccessHandler;
import de.thm.arsnova.security.CasUserDetailsService;
import de.thm.arsnova.security.LoginAuthenticationFailureHandler;
import de.thm.arsnova.security.LoginAuthenticationSucessHandler;
import de.thm.arsnova.security.CustomLdapUserDetailsMapper;
import de.thm.arsnova.security.RegisteredUserDetailsService;
import de.thm.arsnova.security.pac4j.OauthCallbackFilter;
import de.thm.arsnova.security.pac4j.OauthAuthenticationProvider;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.oauth.client.TwitterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads property file and configures components used for authentication.
 */
@Configuration
@EnableGlobalMethodSecurity(mode = AdviceMode.ASPECTJ, prePostEnabled = true)
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	public static final String OAUTH_CALLBACK_PATH_SUFFIX = "/auth/oauth_callback";
	public static final String CAS_LOGIN_PATH_SUFFIX = "/auth/login/cas";
	public static final String CAS_LOGOUT_PATH_SUFFIX = "/auth/logout/cas";
	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

	@Autowired
	private ServletContext servletContext;

	@Value("${root-url}") private String rootUrl;
	@Value("${api.path:}") private String apiPath;

	@Value("${security.user-db.enabled}") private boolean dbAuthEnabled;

	@Value("${security.ldap.enabled}") private boolean ldapEnabled;
	@Value("${security.ldap.url}") private String ldapUrl;
	@Value("${security.ldap.user-id-attr:uid}") private String ldapUserIdAttr;
	@Value("${security.ldap.user-dn-pattern:}") private String ldapUserDn;
	@Value("${security.ldap.user-search-base:}") private String ldapSearchBase;
	@Value("${security.ldap.user-search-filter:}") private String ldapSearchFilter;
	@Value("${security.ldap.manager-user-dn:}") private String ldapManagerUserDn;
	@Value("${security.ldap.manager-password:}") private String ldapManagerPassword;

	@Value("${security.cas.enabled}") private boolean casEnabled;
	@Value("${security.cas-server-url}") private String casUrl;

	@Value("${security.facebook.enabled}") private boolean facebookEnabled;
	@Value("${security.facebook.key}") private String facebookKey;
	@Value("${security.facebook.secret}") private String facebookSecret;

	@Value("${security.twitter.enabled}") private boolean twitterEnabled;
	@Value("${security.twitter.key}") private String twitterKey;
	@Value("${security.twitter.secret}") private String twitterSecret;

	@Value("${security.google.enabled}") private boolean googleEnabled;
	@Value("${security.google.key}") private String googleKey;
	@Value("${security.google.secret}") private String googleSecret;

	@PostConstruct
	private void init() {
		if ("".equals(apiPath)) {
			apiPath = servletContext.getContextPath();
		}
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.exceptionHandling().authenticationEntryPoint(restAuthenticationEntryPoint());
		http.csrf().disable();
		http.headers()
			.addHeaderWriter(new HstsHeaderWriter(false));

		if (casEnabled) {
			http.addFilter(casAuthenticationFilter());
			http.addFilter(casLogoutFilter());
		}

		if (facebookEnabled || googleEnabled || twitterEnabled) {
			http.addFilterAfter(oauthCallbackFilter(), CasAuthenticationFilter.class);
		}
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		List<String> providers = new ArrayList<>();
		if (ldapEnabled) {
			providers.add("ldap");
			auth.authenticationProvider(ldapAuthenticationProvider());
		}
		if (casEnabled) {
			providers.add("cas");
			auth.authenticationProvider(casAuthenticationProvider());
		}
		if (dbAuthEnabled) {
			providers.add("user-db");
			auth.authenticationProvider(daoAuthenticationProvider());
		}
		if (googleEnabled || facebookEnabled || twitterEnabled) {
			if (googleEnabled) {
				providers.add("google");
			}
			if (facebookEnabled) {
				providers.add("facebook");
			}
			if (twitterEnabled) {
				providers.add("twitter");
			}
			auth.authenticationProvider(oauthAuthenticationProvider());
		}
		logger.info("Enabled authentication providers: {}", providers);
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setLocations(
			new ClassPathResource("arsnova.properties.example"),
			new FileSystemResource("file:///etc/arsnova/arsnova.properties")
		);
		configurer.setIgnoreResourceNotFound(true);
		configurer.setIgnoreUnresolvablePlaceholders(false);

		return configurer;
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public static AuthenticationEntryPoint restAuthenticationEntryPoint() {
		return new Http403ForbiddenEntryPoint();
	}

	@Bean
	LoginAuthenticationSucessHandler successHandler() {
		final LoginAuthenticationSucessHandler successHandler = new LoginAuthenticationSucessHandler();
		successHandler.setTargetUrl(rootUrl);

		return successHandler;
	}

	@Bean
	LoginAuthenticationFailureHandler failureHandler() {
		final LoginAuthenticationFailureHandler failureHandler = new LoginAuthenticationFailureHandler();
		failureHandler.setDefaultFailureUrl(rootUrl);

		return failureHandler;
	}

	// Database Authentication Configuration

	@Bean
	public DaoAuthenticationProvider daoAuthenticationProvider() {
		final DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(registeredUserDetailsService());
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public RegisteredUserDetailsService registeredUserDetailsService() {
		return new RegisteredUserDetailsService();
	}

	@Bean
	public SecurityContextLogoutHandler logoutHandler() {
		return new SecurityContextLogoutHandler();
	}

	// LDAP Authentication Configuration

	@Bean
	public LdapAuthenticationProvider ldapAuthenticationProvider() {
		LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(ldapAuthenticator(), ldapAuthoritiesPopulator());
		ldapAuthenticationProvider.setUserDetailsContextMapper(customLdapUserDetailsMapper());

		return ldapAuthenticationProvider;
	}

	@Bean
	public LdapContextSource ldapContextSource() {
		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldapUrl);
		/* TODO: implement support for LDAP bind using manager credentials */
		if (!"".equals(ldapManagerUserDn) && !"".equals(ldapManagerPassword)) {
			logger.debug("ldapManagerUserDn: {}", ldapManagerUserDn);
			contextSource.setUserDn(ldapManagerUserDn);
			contextSource.setPassword(ldapManagerPassword);
		}

		return contextSource;
	}

	@Bean
	public LdapAuthenticator ldapAuthenticator() {
		BindAuthenticator authenticator = new BindAuthenticator(ldapContextSource());
		authenticator.setUserAttributes(new String[] {ldapUserIdAttr});
		if (!"".equals(ldapSearchFilter)) {
			logger.debug("ldapSearch: {} {}", ldapSearchBase, ldapSearchFilter);
			authenticator.setUserSearch(new FilterBasedLdapUserSearch(ldapSearchBase, ldapSearchFilter, ldapContextSource()));
		} else {
			logger.debug("ldapUserDn: {}", ldapUserDn);
			authenticator.setUserDnPatterns(new String[] {ldapUserDn});
		}

		return authenticator;
	}

	@Bean
	public LdapAuthoritiesPopulator ldapAuthoritiesPopulator() {
		return new NullLdapAuthoritiesPopulator();
	}

	@Bean
	public LdapUserDetailsMapper customLdapUserDetailsMapper() {
		logger.debug("ldapUserIdAttr: {}", ldapUserIdAttr);

		return new CustomLdapUserDetailsMapper(ldapUserIdAttr);
	}

	// CAS Authentication Configuration

	@Bean
	public CasAuthenticationProvider casAuthenticationProvider() {
		CasAuthenticationProvider authProvider = new CasAuthenticationProvider();
		authProvider.setAuthenticationUserDetailsService(casUserDetailsService());
		authProvider.setServiceProperties(casServiceProperties());
		authProvider.setTicketValidator(casTicketValidator());
		authProvider.setKey("casAuthProviderKey");

		return authProvider;
	}

	@Bean
	public CasUserDetailsService casUserDetailsService() {
		return new CasUserDetailsService();
	}

	@Bean
	public ServiceProperties casServiceProperties() {
		ServiceProperties properties = new ServiceProperties();
		properties.setService(rootUrl + apiPath + CAS_LOGIN_PATH_SUFFIX);
		properties.setSendRenew(false);

		return properties;
	}

	@Bean
	public Cas20ProxyTicketValidator casTicketValidator() {
		return new Cas20ProxyTicketValidator(casUrl);
	}

	@Bean
	public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
		CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
		entryPoint.setLoginUrl(casUrl + "/login");
		entryPoint.setServiceProperties(casServiceProperties());

		return entryPoint;
	}

	@Bean
	public CasAuthenticationFilter casAuthenticationFilter() throws Exception {
		CasAuthenticationFilter filter = new CasAuthenticationFilter();
		filter.setAuthenticationManager(authenticationManager());
		filter.setServiceProperties(casServiceProperties());
		filter.setFilterProcessesUrl("/**" + CAS_LOGIN_PATH_SUFFIX);
		filter.setAuthenticationSuccessHandler(successHandler());
		filter.setAuthenticationFailureHandler(failureHandler());

		return filter;
	}

	@Bean
	public LogoutFilter casLogoutFilter() {
		LogoutFilter filter = new LogoutFilter(casLogoutSuccessHandler(), logoutHandler());
		filter.setLogoutRequestMatcher(new AntPathRequestMatcher("/**" + CAS_LOGOUT_PATH_SUFFIX));

		return filter;
	}

	@Bean
	public LogoutSuccessHandler casLogoutSuccessHandler() {
		CasLogoutSuccessHandler handler = new CasLogoutSuccessHandler();
		handler.setCasUrl(casUrl);
		handler.setDefaultTarget(rootUrl);

		return handler;
	}

	// OAuth Authentication Configuration

	@Bean
	public Config oauthConfig() {
		List<Client> clients = new ArrayList<>();
		if (facebookEnabled) {
			clients.add(facebookClient());
		}
		if (googleEnabled) {
			clients.add(googleClient());
		}
		if (twitterEnabled) {
			clients.add(twitterClient());
		}

		return new Config(rootUrl + apiPath + OAUTH_CALLBACK_PATH_SUFFIX, clients);
	}

	@Bean
	public OauthCallbackFilter oauthCallbackFilter() throws Exception {
		OauthCallbackFilter callbackFilter = new OauthCallbackFilter(oauthConfig());
		callbackFilter.setAuthenticationManager(authenticationManager());
		callbackFilter.setFilterProcessesUrl("/**" + OAUTH_CALLBACK_PATH_SUFFIX);

		return callbackFilter;
	}

	@Bean
	public OauthAuthenticationProvider oauthAuthenticationProvider() {
		return new OauthAuthenticationProvider();
	}

	@Bean
	public FacebookClient facebookClient() {
		final FacebookClient client = new FacebookClient(facebookKey, facebookSecret);
		client.setCallbackUrl(rootUrl + apiPath + OAUTH_CALLBACK_PATH_SUFFIX + "?client_name=FacebookClient");

		return client;
	}

	@Bean
	public TwitterClient twitterClient() {
		final TwitterClient client = new TwitterClient(twitterKey, twitterSecret);
		client.setCallbackUrl(rootUrl + apiPath + OAUTH_CALLBACK_PATH_SUFFIX + "?client_name=TwitterClient");

		return client;
	}

	@Bean
	public Google2Client googleClient() {
		final Google2Client client = new Google2Client(googleKey, googleSecret);
		client.setCallbackUrl(rootUrl + apiPath + OAUTH_CALLBACK_PATH_SUFFIX + "?client_name=Google2Client");

		return client;
	}
}
