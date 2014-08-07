package de.thm.arsnova.config;

import javax.servlet.ServletContext;

import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.scribe.up.provider.impl.FacebookProvider;
import org.scribe.up.provider.impl.Google2Provider;
import org.scribe.up.provider.impl.Google2Provider.Google2Scope;
import org.scribe.up.provider.impl.TwitterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.ServletContextAware;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationProvider;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationEntryPoint;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationFilter;

import de.thm.arsnova.CASLogoutSuccessHandler;
import de.thm.arsnova.CasUserDetailsService;
import de.thm.arsnova.LoginAuthenticationFailureHandler;
import de.thm.arsnova.LoginAuthenticationSucessHandler;
import de.thm.arsnova.security.DbUserDetailsService;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter implements ServletContextAware {
	private ServletContext servletContext;

	@Value("${root-url}") private String rootUrl;

	@Value("${security.user-db.enabled}") private boolean dbAuthEnabled;

	@Value("${security.ldap.enabled}") private boolean ldapEnabled;
	@Value("${security.ldap.url}") private String ldapUrl;
	@Value("${security.ldap.user-dn-pattern}") private String ldapUserDn;

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

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.exceptionHandling().authenticationEntryPoint(restAuthenticationEntryPoint());
		http.csrf().disable();

		if (casEnabled) {
			http.addFilter(casAuthenticationFilter());
			http.addFilter(casLogoutFilter());
		}
		if (googleEnabled) {
			http.addFilterAfter(googleFilter(), CasAuthenticationFilter.class);
		}
		if (facebookEnabled) {
			http.addFilterAfter(facebookFilter(), CasAuthenticationFilter.class);
		}
		if (twitterEnabled) {
			http.addFilterAfter(twitterFilter(), CasAuthenticationFilter.class);
		}
	};

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		if (dbAuthEnabled) {
			auth.authenticationProvider(daoAuthenticationProvider());
		}
		if (ldapEnabled) {
			auth.authenticationProvider(ldapAuthenticationProvider());
		}
		if (casEnabled) {
			auth.authenticationProvider(casAuthenticationProvider());
		}
		if (googleEnabled) {
			auth.authenticationProvider(googleAuthProvider());
		}
		if (facebookEnabled) {
			auth.authenticationProvider(facebookAuthProvider());
		}
		if (twitterEnabled) {
			auth.authenticationProvider(twitterAuthProvider());
		}
	};

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManager();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setLocations(new Resource[] {
				new ClassPathResource("arsnova.properties.example"),
				new FileSystemResource("file:///etc/arsnova/arsnova.properties"),
		});
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
		authProvider.setUserDetailsService(dbUserDetailsService());
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DbUserDetailsService dbUserDetailsService() {
		return new DbUserDetailsService();
	}

	@Bean
	public SecurityContextLogoutHandler logoutHandler() {
		return new SecurityContextLogoutHandler();
	}

	// LDAP Authentication Configuration

	@Profile("!test")
	@Bean
	public LdapAuthenticationProvider ldapAuthenticationProvider() throws Exception {
		return new LdapAuthenticationProvider(ldapAuthenticator(), ldapAuthoritiesPopulator());
	}

	@Profile("!test")
	@Bean
	public LdapContextSource ldapContextSource() throws Exception {
		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldapUrl);
		/* TODO: implement support for LDAP bind using manager credentials */
//		contextSource.setUserDn(ldapManagerUserDn);
//		contextSource.setPassword(ldapManagerPassword);

		return contextSource;
	}

	@Profile("!test")
	@Bean
	public LdapAuthenticator ldapAuthenticator() throws Exception {
		BindAuthenticator authenticator = new BindAuthenticator(ldapContextSource());
		authenticator.setUserDnPatterns(new String[] {ldapUserDn});

		return authenticator;
	}

	@Profile("!test")
	@Bean
	public LdapAuthoritiesPopulator ldapAuthoritiesPopulator() throws Exception {
		return new DefaultLdapAuthoritiesPopulator(ldapContextSource(), null);
	}

	// CAS Authentication Configuration

	@Profile("!test")
	@Bean
	public CasAuthenticationProvider casAuthenticationProvider() {
		CasAuthenticationProvider authProvider = new CasAuthenticationProvider();
		authProvider.setAuthenticationUserDetailsService(casUserDetailsService());
		authProvider.setServiceProperties(casServiceProperties());
		authProvider.setTicketValidator(casTicketValidator());
		authProvider.setKey("casAuthProviderKey");

		return authProvider;
	}

	@Profile("!test")
	@Bean
	public CasUserDetailsService casUserDetailsService() {
		return new CasUserDetailsService();
	}

	@Profile("!test")
	@Bean
	public ServiceProperties casServiceProperties() {
		ServiceProperties properties = new ServiceProperties();
		properties.setService(rootUrl + servletContext.getContextPath() + "/j_spring_cas_security_check");
		properties.setSendRenew(false);

		return properties;
	}

	@Profile("!test")
	@Bean
	public Cas20ProxyTicketValidator casTicketValidator() {
		return new Cas20ProxyTicketValidator(casUrl);
	}

	@Profile("!test")
	@Bean
	public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
		CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
		entryPoint.setLoginUrl(casUrl + "/login");
		entryPoint.setServiceProperties(casServiceProperties());

		return entryPoint;
	}

	@Profile("!test")
	@Bean
	public CasAuthenticationFilter casAuthenticationFilter() throws Exception {
		CasAuthenticationFilter filter = new CasAuthenticationFilter();
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationSuccessHandler(successHandler());
		filter.setAuthenticationFailureHandler(failureHandler());

		return filter;
	}

	@Profile("!test")
	@Bean
	public LogoutFilter casLogoutFilter() {
		LogoutFilter filter = new LogoutFilter(casLogoutSuccessHandler(), logoutHandler());
		filter.setLogoutRequestMatcher(new AntPathRequestMatcher("/j_spring_cas_security_logout"));

		return filter;
	}

	@Profile("!test")
	@Bean
	public LogoutSuccessHandler casLogoutSuccessHandler() {
		CASLogoutSuccessHandler handler = new CASLogoutSuccessHandler();
		handler.setCasUrl(casUrl);
		handler.setDefaultTarget(rootUrl);

		return handler;
	}

	// Facebook Authentication Configuration

	@Profile("!test")
	@Bean
	public OAuthAuthenticationEntryPoint facebookEntryPoint() {
		final OAuthAuthenticationEntryPoint entryPoint = new OAuthAuthenticationEntryPoint();
		entryPoint.setProvider(facebookProvider());

		return entryPoint;
	}

	@Profile("!test")
	@Bean
	public FacebookProvider facebookProvider() {
		final FacebookProvider provider = new FacebookProvider();
		provider.setKey(facebookKey);
		provider.setSecret(facebookSecret);
		provider.setCallbackUrl(rootUrl + servletContext.getContextPath() + "/j_spring_facebook_security_check");

		return provider;
	}

	@Profile("!test")
	@Bean
	public OAuthAuthenticationFilter facebookFilter() throws Exception {
		final OAuthAuthenticationFilter filter = new OAuthAuthenticationFilter("/j_spring_facebook_security_check");
		filter.setProvider(facebookProvider());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(failureHandler());
		filter.setAuthenticationSuccessHandler(successHandler());

		return filter;
	}

	@Profile("!test")
	@Bean
	public OAuthAuthenticationProvider facebookAuthProvider() {
		final OAuthAuthenticationProvider authProvider = new OAuthAuthenticationProvider();
		authProvider.setProvider(facebookProvider());

		return authProvider;
	}

	// Twitter Authentication Configuration

	@Profile("!test")
	@Bean
	public TwitterProvider twitterProvider() {
		final TwitterProvider provider = new TwitterProvider();
		provider.setKey(twitterKey);
		provider.setSecret(twitterSecret);
		provider.setCallbackUrl(rootUrl + servletContext.getContextPath() + "/j_spring_twitter_security_check");

		return provider;
	}

	@Profile("!test")
	@Bean
	public OAuthAuthenticationFilter twitterFilter() throws Exception {
		final OAuthAuthenticationFilter filter = new OAuthAuthenticationFilter("/j_spring_twitter_security_check");
		filter.setProvider(twitterProvider());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(failureHandler());
		filter.setAuthenticationSuccessHandler(successHandler());
		return filter;
	}

	@Profile("!test")
	@Bean
	public OAuthAuthenticationProvider twitterAuthProvider() {
		final OAuthAuthenticationProvider authProvider = new OAuthAuthenticationProvider();
		authProvider.setProvider(twitterProvider());

		return authProvider;
	}

	// Google Authentication Configuration

	@Profile("!test")
	@Bean
	public Google2Provider googleProvider() {
		final Google2Provider provider = new Google2Provider();
		provider.setKey(googleKey);
		provider.setSecret(googleSecret);
		provider.setCallbackUrl(rootUrl + servletContext.getContextPath() + "/j_spring_google_security_check");
		provider.setScope(Google2Scope.EMAIL);

		return provider;
	}

	@Profile("!test")
	@Bean
	public OAuthAuthenticationFilter googleFilter() throws Exception {
		final OAuthAuthenticationFilter filter = new OAuthAuthenticationFilter("/j_spring_google_security_check");
		filter.setProvider(googleProvider());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(failureHandler());
		filter.setAuthenticationSuccessHandler(successHandler());

		return filter;
	}

	@Profile("!test")
	@Bean
	public OAuthAuthenticationProvider googleAuthProvider() {
		final OAuthAuthenticationProvider authProvider = new OAuthAuthenticationProvider();
		authProvider.setProvider(googleProvider());

		return authProvider;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
}
