/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.access.intercept.RunAsManagerImpl;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
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
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.controller.ControllerExceptionHelper;
import de.thm.arsnova.security.CasLogoutSuccessHandler;
import de.thm.arsnova.security.CasUserDetailsService;
import de.thm.arsnova.security.CustomLdapUserDetailsMapper;
import de.thm.arsnova.security.LoginAuthenticationFailureHandler;
import de.thm.arsnova.security.LoginAuthenticationSucessHandler;
import de.thm.arsnova.security.RegisteredUserDetailsService;
import de.thm.arsnova.security.jwt.JwtAuthenticationProvider;
import de.thm.arsnova.security.jwt.JwtTokenFilter;
import de.thm.arsnova.security.pac4j.OauthAuthenticationProvider;
import de.thm.arsnova.security.pac4j.OauthCallbackFilter;

/**
 * Loads property file and configures components used for authentication.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
		AuthenticationProviderProperties.class,
		SecurityProperties.class})
@Profile("!test")
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	public static final String AUTH_CALLBACK_PATH = "/auth/callback";
	public static final String OAUTH_CALLBACK_PATH = AUTH_CALLBACK_PATH + "/oauth";
	public static final String CAS_CALLBACK_PATH = OAUTH_CALLBACK_PATH + "/cas";
	public static final String CAS_LOGOUT_PATH = "/auth/logout/cas";
	public static final String RUN_AS_KEY_PREFIX = "RUN_AS_KEY";
	public static final String INTERNAL_PROVIDER_ID = "user-db";
	public static final String LDAP_PROVIDER_ID = "ldap";
	public static final String OIDC_PROVIDER_ID = "oidc";
	public static final String CAS_PROVIDER_ID = "cas";
	public static final String GOOGLE_PROVIDER_ID = "google";
	public static final String TWITTER_PROVIDER_ID = "twitter";
	public static final String FACEBOOK_PROVIDER_ID = "facebook";
	private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";
	private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

	private ServletContext servletContext;
	private AuthenticationProviderProperties providerProperties;
	private String rootUrl;
	private String apiPath;

	public SecurityConfig(
			final SystemProperties systemProperties,
			final AuthenticationProviderProperties authenticationProviderProperties,
			final ServletContext servletContext) {
		this.providerProperties = authenticationProviderProperties;
		this.rootUrl = systemProperties.getRootUrl();
		this.apiPath = systemProperties.getApi().getProxyPath();
		this.servletContext = servletContext;
	}

	@PostConstruct
	private void init() {
		if (apiPath == null || "".equals(apiPath)) {
			apiPath = servletContext.getContextPath();
		}
	}

	public class HttpSecurityConfig extends WebSecurityConfigurerAdapter {
		protected AuthenticationEntryPoint authenticationEntryPoint;
		protected AccessDeniedHandler accessDeniedHandler;

		public HttpSecurityConfig(final AuthenticationEntryPoint authenticationEntryPoint,
				final AccessDeniedHandler accessDeniedHandler) {
			this.authenticationEntryPoint = authenticationEntryPoint;
			this.accessDeniedHandler = accessDeniedHandler;
		}

		@Override
		protected void configure(final HttpSecurity http) throws Exception {
			http.exceptionHandling()
					.authenticationEntryPoint(authenticationEntryPoint)
					.accessDeniedHandler(accessDeniedHandler);
			http.csrf().disable();
			http.headers().addHeaderWriter(new HstsHeaderWriter(false));

			http.addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
			if (providerProperties.getCas().isEnabled()) {
				http.addFilter(casAuthenticationFilter());
				http.addFilter(casLogoutFilter());
			}

			if (providerProperties.getOidc().stream().anyMatch(p -> p.isEnabled())
					|| providerProperties.getOauth().values().stream().anyMatch(p -> p.isEnabled())) {
				http.addFilterAfter(oauthCallbackFilter(), UsernamePasswordAuthenticationFilter.class);
			}
		}
	}

	@Configuration
	@Order(2)
	@Profile("!test")
	public class StatelessHttpSecurityConfig extends HttpSecurityConfig {
		public StatelessHttpSecurityConfig(
				@Qualifier("restAuthenticationEntryPoint") final AuthenticationEntryPoint authenticationEntryPoint,
				final AccessDeniedHandler accessDeniedHandler) {
			super(authenticationEntryPoint, accessDeniedHandler);
		}

		@Override
		protected void configure(final HttpSecurity http) throws Exception {
			super.configure(http);
			http.antMatcher("/**");
			http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		}
	}

	@Configuration
	@Order(1)
	@Profile("!test")
	public class StatefulHttpSecurityConfig extends HttpSecurityConfig {
		public StatefulHttpSecurityConfig(
				@Qualifier("restAuthenticationEntryPoint") final AuthenticationEntryPoint authenticationEntryPoint,
				final AccessDeniedHandler accessDeniedHandler) {
			super(authenticationEntryPoint, accessDeniedHandler);
		}

		@Override
		protected void configure(final HttpSecurity http) throws Exception {
			super.configure(http);
			http.requestMatchers().antMatchers(AUTH_CALLBACK_PATH + "/**", "/v2/**");
			http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
		}
	}

	@Configuration
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@Profile("!test")
	public class ManagementHttpSecurityConfig extends HttpSecurityConfig {
		private final String managementPath;

		public ManagementHttpSecurityConfig(
				@Qualifier("restAuthenticationEntryPoint") final AuthenticationEntryPoint authenticationEntryPoint,
				final AccessDeniedHandler accessDeniedHandler,
				final WebEndpointProperties webEndpointProperties) {
			super(authenticationEntryPoint, accessDeniedHandler);
			this.managementPath = webEndpointProperties.getBasePath();
		}

		@Override
		protected void configure(final HttpSecurity http) throws Exception {
			super.configure(http);
			http.antMatcher(managementPath + "/**");
			http.authorizeRequests().anyRequest().hasRole("ADMIN");
			http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		}
	}

	@Configuration
	@EnableGlobalMethodSecurity(mode = AdviceMode.ASPECTJ, prePostEnabled = true, securedEnabled = true)
	public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
		@Override
		protected RunAsManager runAsManager() {
			final StringKeyGenerator keyGenerator = new Base64StringKeyGenerator();
			final RunAsManagerImpl runAsManager = new RunAsManagerImpl();
			/* Since RunAsTokens should currently only be used internally, we generate a random key. */
			runAsManager.setKey(RUN_AS_KEY_PREFIX + keyGenerator.generateKey());

			return runAsManager;
		}
	}

	@Override
	protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
		final List<String> providers = new ArrayList<>();
		auth.authenticationProvider(jwtAuthenticationProvider());
		logger.info("oauthProps: {}", providerProperties.getOauth());
		if (providerProperties.getLdap().stream().anyMatch(p -> p.isEnabled())) {
			providers.add(LDAP_PROVIDER_ID);
			auth.authenticationProvider(ldapAuthenticationProvider());
		}
		if (providerProperties.getCas().isEnabled()) {
			providers.add(CAS_PROVIDER_ID);
			auth.authenticationProvider(casAuthenticationProvider());
		}
		if (providerProperties.getRegistered().isEnabled()) {
			providers.add(INTERNAL_PROVIDER_ID);
			auth.authenticationProvider(daoAuthenticationProvider());
		}
		boolean oauthOrOidcProvider = false;
		if (providerProperties.getOidc().stream().anyMatch(p -> p.isEnabled())) {
			oauthOrOidcProvider = true;
			providers.add(OIDC_PROVIDER_ID);
		}
		if (providerProperties.getOauth().values().stream().anyMatch(p -> p.isEnabled())) {
			oauthOrOidcProvider = true;
			if (providerProperties.getOauth().containsKey(GOOGLE_PROVIDER_ID)
					&& providerProperties.getOauth().get(GOOGLE_PROVIDER_ID).isEnabled()) {
				providers.add(GOOGLE_PROVIDER_ID);
			}
			if (providerProperties.getOauth().containsKey(FACEBOOK_PROVIDER_ID)
					&& providerProperties.getOauth().get(FACEBOOK_PROVIDER_ID).isEnabled()) {
				providers.add(FACEBOOK_PROVIDER_ID);
			}
			if (providerProperties.getOauth().containsKey(TWITTER_PROVIDER_ID)
					&& providerProperties.getOauth().get(TWITTER_PROVIDER_ID).isEnabled()) {
				providers.add(TWITTER_PROVIDER_ID);
			}
		}
		if (oauthOrOidcProvider) {
			auth.authenticationProvider(oauthAuthenticationProvider());
		}
		logger.info("Enabled authentication providers: {}", providers);
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public static AuthenticationEntryPoint restAuthenticationEntryPoint(
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final ControllerExceptionHelper controllerExceptionHelper) {
		return (request, response, accessDeniedException) -> {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_UTF8.toString());
			response.getWriter().write(jackson2HttpMessageConverter.getObjectMapper().writeValueAsString(
					controllerExceptionHelper.handleException(accessDeniedException, Level.DEBUG)));
		};
	}

	@Bean
	public AccessDeniedHandler customAccessDeniedHandler(
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final ControllerExceptionHelper controllerExceptionHelper) {
		return (request, response, accessDeniedException) -> {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_UTF8.toString());
			response.getWriter().write(jackson2HttpMessageConverter.getObjectMapper().writeValueAsString(
					controllerExceptionHelper.handleException(accessDeniedException, Level.DEBUG)));
		};
	}

	@Bean
	public JwtAuthenticationProvider jwtAuthenticationProvider() {
		return new JwtAuthenticationProvider();
	}

	@Bean
	public JwtTokenFilter jwtTokenFilter() throws Exception {
		final JwtTokenFilter jwtTokenFilter = new JwtTokenFilter();
		return jwtTokenFilter;
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
	@ConditionalOnProperty(
			name = "ldap[0].enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public LdapAuthenticationProvider ldapAuthenticationProvider() {
		final LdapAuthenticationProvider ldapAuthenticationProvider =
				new LdapAuthenticationProvider(ldapAuthenticator(), ldapAuthoritiesPopulator());
		ldapAuthenticationProvider.setUserDetailsContextMapper(customLdapUserDetailsMapper());

		return ldapAuthenticationProvider;
	}

	@Bean
	@ConditionalOnProperty(
			name = "ldap[0].enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public LdapContextSource ldapContextSource() {
		final AuthenticationProviderProperties.Ldap ldapProperties = providerProperties.getLdap().get(0);
		final DefaultSpringSecurityContextSource contextSource =
				new DefaultSpringSecurityContextSource(ldapProperties.getHostUrl());
		/* TODO: implement support for LDAP bind using manager credentials */
		if (!"".equals(ldapProperties.getManagerUserDn()) && !"".equals(ldapProperties.getManagerPassword())) {
			logger.debug("ldapManagerUserDn: {}", ldapProperties.getManagerUserDn());
			contextSource.setUserDn(ldapProperties.getManagerUserDn());
			contextSource.setPassword(ldapProperties.getManagerPassword());
		}

		return contextSource;
	}

	@Bean
	@ConditionalOnProperty(
			name = "ldap[0].enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public LdapAuthenticator ldapAuthenticator() {
		final AuthenticationProviderProperties.Ldap ldapProperties = providerProperties.getLdap().get(0);
		final BindAuthenticator authenticator = new BindAuthenticator(ldapContextSource());
		authenticator.setUserAttributes(new String[] {ldapProperties.getUserIdAttribute()});
		if (!"".equals(ldapProperties.getUserSearchFilter())) {
			logger.debug("ldapSearch: {} {}", ldapProperties.getUserSearchBase(), ldapProperties.getUserSearchFilter());
			authenticator.setUserSearch(new FilterBasedLdapUserSearch(
					ldapProperties.getUserSearchBase(), ldapProperties.getUserSearchFilter(), ldapContextSource()));
		} else {
			logger.debug("ldapUserDn: {}", ldapProperties.getUserDnPattern());
			authenticator.setUserDnPatterns(new String[] {ldapProperties.getUserDnPattern()});
		}

		return authenticator;
	}

	@Bean
	@ConditionalOnProperty(
			name = "ldap[0].enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public LdapAuthoritiesPopulator ldapAuthoritiesPopulator() {
		return new NullLdapAuthoritiesPopulator();
	}

	@Bean
	@ConditionalOnProperty(
			name = "ldap[0].enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public LdapUserDetailsMapper customLdapUserDetailsMapper() {
		final AuthenticationProviderProperties.Ldap ldapProperties = providerProperties.getLdap().get(0);
		logger.debug("ldapUserIdAttr: {}", ldapProperties.getUserIdAttribute());

		return new CustomLdapUserDetailsMapper(ldapProperties.getUserIdAttribute());
	}

	// CAS Authentication Configuration

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public CasAuthenticationProvider casAuthenticationProvider() {
		final CasAuthenticationProvider authProvider = new CasAuthenticationProvider();
		authProvider.setAuthenticationUserDetailsService(casUserDetailsService());
		authProvider.setServiceProperties(casServiceProperties());
		authProvider.setTicketValidator(casTicketValidator());
		authProvider.setKey("casAuthProviderKey");

		return authProvider;
	}

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public CasUserDetailsService casUserDetailsService() {
		return new CasUserDetailsService();
	}

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public ServiceProperties casServiceProperties() {
		final ServiceProperties properties = new ServiceProperties();
		properties.setService(rootUrl + apiPath + CAS_CALLBACK_PATH);
		properties.setSendRenew(false);

		return properties;
	}

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public Cas20ProxyTicketValidator casTicketValidator() {
		return new Cas20ProxyTicketValidator(providerProperties.getCas().getHostUrl());
	}

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
		final CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
		entryPoint.setLoginUrl(providerProperties.getCas().getHostUrl() + "/login");
		entryPoint.setServiceProperties(casServiceProperties());

		return entryPoint;
	}

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public CasAuthenticationFilter casAuthenticationFilter() throws Exception {
		final CasAuthenticationFilter filter = new CasAuthenticationFilter();
		filter.setAuthenticationManager(authenticationManager());
		filter.setServiceProperties(casServiceProperties());
		filter.setFilterProcessesUrl("/**" + CAS_CALLBACK_PATH);
		filter.setAuthenticationSuccessHandler(successHandler());
		filter.setAuthenticationFailureHandler(failureHandler());

		return filter;
	}

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public LogoutFilter casLogoutFilter() {
		final LogoutFilter filter = new LogoutFilter(casLogoutSuccessHandler(), logoutHandler());
		filter.setLogoutRequestMatcher(new AntPathRequestMatcher("/**" + CAS_LOGOUT_PATH));

		return filter;
	}

	@Bean
	@ConditionalOnProperty(
			name = "cas.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public LogoutSuccessHandler casLogoutSuccessHandler() {
		final CasLogoutSuccessHandler handler = new CasLogoutSuccessHandler();
		handler.setCasUrl(providerProperties.getCas().getHostUrl());
		handler.setDefaultTarget(rootUrl);

		return handler;
	}

	// OAuth Authentication Configuration

	@Bean
	public Config oauthConfig() {
		final List<Client> clients = new ArrayList<>();
		if (providerProperties.getOidc().stream().anyMatch(p -> p.isEnabled())) {
			clients.add(oidcClient());
		}
		if (providerProperties.getOauth().containsKey(FACEBOOK_PROVIDER_ID)
				&& providerProperties.getOauth().get(FACEBOOK_PROVIDER_ID).isEnabled()) {
			clients.add(facebookClient());
		}
		if (providerProperties.getOauth().containsKey(GOOGLE_PROVIDER_ID)
				&& providerProperties.getOauth().get(GOOGLE_PROVIDER_ID).isEnabled()) {
			clients.add(googleClient());
		}
		if (providerProperties.getOauth().containsKey(TWITTER_PROVIDER_ID)
				&& providerProperties.getOauth().get(TWITTER_PROVIDER_ID).isEnabled()) {
			clients.add(twitterClient());
		}

		return new Config(rootUrl + apiPath + OAUTH_CALLBACK_PATH, clients);
	}

	@Bean
	public OauthCallbackFilter oauthCallbackFilter() throws Exception {
		final OauthCallbackFilter callbackFilter = new OauthCallbackFilter(oauthConfig(), OAUTH_CALLBACK_PATH);
		callbackFilter.setAuthenticationManager(authenticationManager());

		return callbackFilter;
	}

	@Bean
	public OauthAuthenticationProvider oauthAuthenticationProvider() {
		return new OauthAuthenticationProvider();
	}

	@Bean
	public PathParameterCallbackUrlResolver pathParameterCallbackUrlResolver() {
		return new PathParameterCallbackUrlResolver();
	}

	@Bean
	@ConditionalOnProperty(
			name = "oidc[0].enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public OidcClient oidcClient() {
		final AuthenticationProviderProperties.Oidc oidcProperties = providerProperties.getOidc().get(0);
		final OidcConfiguration config = new OidcConfiguration();
		config.setDiscoveryURI(oidcProperties.getIssuer() + OIDC_DISCOVERY_PATH);
		config.setClientId(oidcProperties.getClientId());
		config.setSecret(oidcProperties.getSecret());
		config.setScope("openid");
		final OidcClient client = new OidcClient(config);
		client.setName(OIDC_PROVIDER_ID);
		client.setCallbackUrl(rootUrl + apiPath + OAUTH_CALLBACK_PATH);
		client.setCallbackUrlResolver(pathParameterCallbackUrlResolver());

		return client;
	}

	@Bean
	@ConditionalOnProperty(
			name = "oauth.facebook.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public FacebookClient facebookClient() {
		final AuthenticationProviderProperties.Oauth oauthProperties =
				providerProperties.getOauth().get(FACEBOOK_PROVIDER_ID);
		final FacebookClient client = new FacebookClient(oauthProperties.getKey(), oauthProperties.getSecret());
		client.setName(FACEBOOK_PROVIDER_ID);
		client.setCallbackUrl(rootUrl + apiPath + OAUTH_CALLBACK_PATH);
		client.setCallbackUrlResolver(pathParameterCallbackUrlResolver());

		return client;
	}

	@Bean
	@ConditionalOnProperty(
			name = "oauth.twitter.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public TwitterClient twitterClient() {
		final AuthenticationProviderProperties.Oauth oauthProperties =
				providerProperties.getOauth().get(TWITTER_PROVIDER_ID);
		final TwitterClient client = new TwitterClient(oauthProperties.getKey(), oauthProperties.getSecret());
		client.setName(TWITTER_PROVIDER_ID);
		client.setCallbackUrl(rootUrl + apiPath + OAUTH_CALLBACK_PATH);
		client.setCallbackUrlResolver(pathParameterCallbackUrlResolver());

		return client;
	}

	@Bean
	@ConditionalOnProperty(
			name = "oauth.google.enabled",
			prefix = AuthenticationProviderProperties.PREFIX,
			havingValue = "true")
	public GoogleOidcClient googleClient() {
		final AuthenticationProviderProperties.Oauth oauthProperties =
				providerProperties.getOauth().get(GOOGLE_PROVIDER_ID);
		final OidcConfiguration config = new OidcConfiguration();
		config.setClientId(oauthProperties.getKey());
		config.setSecret(oauthProperties.getSecret());
		config.setScope("openid email");
		final GoogleOidcClient client = new GoogleOidcClient(config);
		client.setName(GOOGLE_PROVIDER_ID);
		client.setCallbackUrl(rootUrl + apiPath + OAUTH_CALLBACK_PATH);
		client.setCallbackUrlResolver(pathParameterCallbackUrlResolver());

		return client;
	}
}
