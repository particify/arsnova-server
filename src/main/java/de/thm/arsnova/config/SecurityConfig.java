package de.thm.arsnova.config;

import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.scribe.up.provider.impl.FacebookProvider;
import org.scribe.up.provider.impl.Google2Provider;
import org.scribe.up.provider.impl.Google2Provider.Google2Scope;
import org.scribe.up.provider.impl.TwitterProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.support.DirContextSource;
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
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationProvider;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationEntryPoint;
import com.github.leleuj.ss.oauth.client.web.OAuthAuthenticationFilter;

import de.thm.arsnova.CasUserDetailsService;
import de.thm.arsnova.LoginAuthenticationFailureHandler;
import de.thm.arsnova.LoginAuthenticationSucessHandler;
import de.thm.arsnova.security.DbUserDetailsService;

@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("$security.ldap.server-url") private String ldapUrl;
	@Value("$security.ldap.userDn") private String ldapUserDn;

	@Value("${security.cas-server-url}") private String casUrl;

	@Value(value = "${security.facebook.key}") private String securityFacebookKey;
	@Value(value = "${security.facebook.secret}") private String securityFacebookSecret;

	@Value(value = "${security.twitter.key}") private String securityTwitterKey;
	@Value(value = "${security.twitter.secret}") private String securityTwitterSecret;

	@Value(value = "${security.google.key}") private String securityGoogleKey;
	@Value(value = "${security.google.secret}") private String securityGoogleSecret;

	@Value("${root-url}") private String rootUrl;
	@Value("#{servletContext.contextPath}") private String contextPath;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.exceptionHandling().authenticationEntryPoint(restAuthenticationEntryPoint());
		http.addFilter(casAuthenticationFilter());
		http.addFilterAfter(googleFilter(), CasAuthenticationFilter.class);
		http.addFilterAfter(facebookFilter(), CasAuthenticationFilter.class);
		http.addFilterAfter(twitterFilter(), CasAuthenticationFilter.class);
	};

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(daoAuthenticationProvider());
		auth.authenticationProvider(ldapAuthenticationProvider());
		auth.authenticationProvider(casAuthenticationProvider());
		auth.authenticationProvider(googleAuthProvider());
		auth.authenticationProvider(facebookAuthProvider());
		auth.authenticationProvider(twitterAuthProvider());
	};

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		final AuthenticationManager authenticationManager = super.authenticationManager();
		return authenticationManager;
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
		successHandler.setTargetUrl("#auth/checkLogin");
		return successHandler;
	}

	@Bean
	LoginAuthenticationFailureHandler failureHandler() {
		final LoginAuthenticationFailureHandler failureHandler = new LoginAuthenticationFailureHandler();
		failureHandler.setDefaultFailureUrl("/index.html");
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

	// LDAP Authentication Configuration

	@Bean
	public LdapAuthenticationProvider ldapAuthenticationProvider() {
		DirContextSource contextSource = new DirContextSource();
		contextSource.setUrl(ldapUrl);
		contextSource.setUserDn(ldapUserDn);
		LdapAuthenticator authenticator = new BindAuthenticator(contextSource);
		LdapAuthenticationProvider authProvider = new LdapAuthenticationProvider(authenticator, new NullLdapAuthoritiesPopulator());

		return authProvider;
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
		properties.setService(rootUrl + contextPath + "/j_spring_cas_security_check");
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
		filter.setAuthenticationSuccessHandler(successHandler());
		filter.setAuthenticationFailureHandler(failureHandler());

		return filter;
	}

	// Facebook Authentication Configuration

	@Bean
	public OAuthAuthenticationEntryPoint facebookEntryPoint() {
		final OAuthAuthenticationEntryPoint entryPoint = new OAuthAuthenticationEntryPoint();
		entryPoint.setProvider(facebookProvider());
		return entryPoint;
	}

	@Bean
	public FacebookProvider facebookProvider() {
		final FacebookProvider provider = new FacebookProvider();
		provider.setKey(securityFacebookKey);
		provider.setSecret(securityFacebookSecret);
		provider.setCallbackUrl(rootUrl + "/j_spring_facebook_security_check");
		return provider;
	}

	@Bean
	public OAuthAuthenticationFilter facebookFilter() throws Exception {
		final OAuthAuthenticationFilter filter = new OAuthAuthenticationFilter("/j_spring_facebook_security_check");
		filter.setFilterProcessesUrl("/j_spring_facebook_security_check");
		filter.setProvider(facebookProvider());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(failureHandler());
		filter.setAuthenticationSuccessHandler(successHandler());
		return filter;
	}

	@Bean
	public OAuthAuthenticationProvider facebookAuthProvider() {
		final OAuthAuthenticationProvider authProvider = new OAuthAuthenticationProvider();
		authProvider.setProvider(facebookProvider());
		return authProvider;
	}

	// Twitter Authentication Configuration

	@Bean
	public TwitterProvider twitterProvider() {
		final TwitterProvider provider = new TwitterProvider();
		provider.setKey(securityTwitterKey);
		provider.setSecret(securityTwitterSecret);
		provider.setCallbackUrl(rootUrl + "/j_spring_twitter_security_check");
		return provider;
	}

	@Bean
	public OAuthAuthenticationFilter twitterFilter() throws Exception {
		final OAuthAuthenticationFilter filter = new OAuthAuthenticationFilter("/j_spring_twitter_security_check");
		filter.setFilterProcessesUrl("/j_spring_twitter_security_check");
		filter.setProvider(twitterProvider());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(failureHandler());
		filter.setAuthenticationSuccessHandler(successHandler());
		return filter;
	}

	@Bean
	public OAuthAuthenticationProvider twitterAuthProvider() {
		final OAuthAuthenticationProvider authProvider = new OAuthAuthenticationProvider();
		authProvider.setProvider(twitterProvider());
		return authProvider;
	}

	// Google Authentication Configuration

	@Bean
	public Google2Provider googleProvider() {
		final Google2Provider provider = new Google2Provider();
		provider.setKey(securityGoogleKey);
		provider.setSecret(securityGoogleSecret);
		provider.setCallbackUrl(rootUrl + "/j_spring_google_security_check");
		provider.setScope(Google2Scope.EMAIL);
		return provider;
	}

	@Bean
	public OAuthAuthenticationFilter googleFilter() throws Exception {
		final OAuthAuthenticationFilter filter = new OAuthAuthenticationFilter("/j_spring_google_security_check");
		filter.setFilterProcessesUrl("/j_spring_google_security_check");
		filter.setProvider(twitterProvider());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(failureHandler());
		filter.setAuthenticationSuccessHandler(successHandler());
		return filter;
	}

	@Bean
	public OAuthAuthenticationProvider googleAuthProvider() {
		final OAuthAuthenticationProvider authProvider = new OAuthAuthenticationProvider();
		authProvider.setProvider(googleProvider());
		return authProvider;
	}
}
