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

package net.particify.arsnova.core.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apereo.cas.client.validation.Cas20ProxyTicketValidator;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
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
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.access.intercept.RunAsManagerImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import net.particify.arsnova.core.config.properties.AuthenticationProviderProperties;
import net.particify.arsnova.core.config.properties.SecurityProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.controller.ControllerExceptionHelper;
import net.particify.arsnova.core.security.CasLogoutSuccessHandler;
import net.particify.arsnova.core.security.CasUserDetailsService;
import net.particify.arsnova.core.security.CustomLdapUserDetailsMapper;
import net.particify.arsnova.core.security.LoginAuthenticationFailureHandler;
import net.particify.arsnova.core.security.LoginAuthenticationSucessHandler;
import net.particify.arsnova.core.security.RegisteredUserDetailsService;
import net.particify.arsnova.core.security.jwt.JwtAuthenticationProvider;
import net.particify.arsnova.core.security.jwt.JwtTokenFilter;
import net.particify.arsnova.core.security.pac4j.SsoAuthenticationProvider;
import net.particify.arsnova.core.security.pac4j.SsoCallbackFilter;

/**
 * Loads property file and configures components used for authentication.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({
    AuthenticationProviderProperties.class,
    SecurityProperties.class})
@Profile("!test")
public class SecurityConfig {
  public static final String AUTH_CALLBACK_PATH = "/auth/callback";
  public static final String OAUTH_CALLBACK_PATH = AUTH_CALLBACK_PATH + "/oauth";
  public static final String SAML_CALLBACK_PATH = AUTH_CALLBACK_PATH + "/saml";
  public static final String CAS_CALLBACK_PATH = AUTH_CALLBACK_PATH + "/cas";
  public static final String CAS_LOGOUT_PATH = "/auth/logout/cas";
  public static final String RUN_AS_KEY_PREFIX = "RUN_AS_KEY";
  public static final String INTERNAL_PROVIDER_ID = "user-db";
  public static final String LDAP_PROVIDER_ID = "ldap";
  public static final String OIDC_PROVIDER_ID = "oidc";
  public static final String SAML_PROVIDER_ID = "saml";
  public static final String CAS_PROVIDER_ID = "cas";
  private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";
  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  private ServletContext servletContext;
  private SystemProperties systemProperties;
  private AuthenticationProviderProperties providerProperties;
  private String rootUrl;
  private String apiPath;
  private JwtTokenFilter jwtTokenFilter;
  private JwtAuthenticationProvider jwtAuthenticationProvider;
  private RegisteredUserDetailsService registeredUserDetailsService;
  private SsoAuthenticationProvider ssoAuthenticationProvider;

  public SecurityConfig(
      final SystemProperties systemProperties,
      final AuthenticationProviderProperties authenticationProviderProperties,
      final ServletContext servletContext) {
    this.systemProperties = systemProperties;
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

  @Autowired
  public void setJwtTokenFilter(final JwtTokenFilter jwtTokenFilter) {
    this.jwtTokenFilter = jwtTokenFilter;
  }

  @Autowired
  public void setJwtAuthenticationProvider(final JwtAuthenticationProvider jwtAuthenticationProvider) {
    this.jwtAuthenticationProvider = jwtAuthenticationProvider;
  }

  @Autowired
  public void setRegisteredUserDetailsService(final RegisteredUserDetailsService registeredUserDetailsService) {
    this.registeredUserDetailsService = registeredUserDetailsService;
  }

  @Autowired
  public void setSsoAuthenticationProvider(final SsoAuthenticationProvider ssoAuthenticationProvider) {
    this.ssoAuthenticationProvider = ssoAuthenticationProvider;
  }

  public class HttpSecurityConfig {
    protected AuthenticationEntryPoint authenticationEntryPoint;
    protected AccessDeniedHandler accessDeniedHandler;

    public HttpSecurityConfig(final AuthenticationEntryPoint authenticationEntryPoint,
        final AccessDeniedHandler accessDeniedHandler) {
      this.authenticationEntryPoint = authenticationEntryPoint;
      this.accessDeniedHandler = accessDeniedHandler;
    }

    protected HttpSecurity configureFilterChain(final HttpSecurity http) throws Exception {
      http.exceptionHandling()
          .authenticationEntryPoint(authenticationEntryPoint)
          .accessDeniedHandler(accessDeniedHandler);
      http.csrf().disable();
      http.headers().addHeaderWriter(new HstsHeaderWriter(false));

      http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
      if (providerProperties.getCas().isEnabled()) {
        http.addFilter(casAuthenticationFilter());
        http.addFilter(casLogoutFilter());
      }
      if (providerProperties.getSaml().isEnabled()) {
        http.addFilterAfter(samlCallbackFilter(), UsernamePasswordAuthenticationFilter.class);
      }
      if (providerProperties.getOidc().stream().anyMatch(p -> p.isEnabled())) {
        http.addFilterAfter(oauthCallbackFilter(), UsernamePasswordAuthenticationFilter.class);
      }

      return http;
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

    @Bean
    public SecurityFilterChain statelessFilterChain(final HttpSecurity http) throws Exception {
      super.configureFilterChain(http);
      http.securityMatcher("/**");
      http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

      return http.build();
    }
  }

  @Configuration
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Profile("!test")
  public class ManagementHttpSecurityConfig extends HttpSecurityConfig {
    public ManagementHttpSecurityConfig(
        @Qualifier("restAuthenticationEntryPoint") final AuthenticationEntryPoint authenticationEntryPoint,
        final AccessDeniedHandler accessDeniedHandler) {
      super(authenticationEntryPoint, accessDeniedHandler);
    }

    @Bean
    public SecurityFilterChain managementFilterChain(final HttpSecurity http) throws Exception {
      super.configureFilterChain(http);
      http.securityMatcher(EndpointRequest.toAnyEndpoint());
      http.authorizeHttpRequests()
          .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
          .requestMatchers(
            EndpointRequest.to("metrics", "prometheus", "stats")
          ).hasAnyRole("ADMIN", "MONITORING")
          .anyRequest().hasRole("ADMIN");
      http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

      return http.build();
    }
  }

  @Configuration
  @EnableGlobalMethodSecurity(mode = AdviceMode.ASPECTJ, prePostEnabled = true, securedEnabled = true)
  public static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
    @Override
    protected RunAsManager runAsManager() {
      final StringKeyGenerator keyGenerator = new Base64StringKeyGenerator();
      final RunAsManagerImpl runAsManager = new RunAsManagerImpl();
      /* Since RunAsTokens should currently only be used internally, we generate a random key. */
      runAsManager.setKey(RUN_AS_KEY_PREFIX + keyGenerator.generateKey());

      return runAsManager;
    }
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    final List<AuthenticationProvider> providers = new ArrayList<>();
    final List<String> providerIds = new ArrayList<>();
    providers.add(jwtAuthenticationProvider);
    if (providerProperties.getLdap().stream().anyMatch(p -> p.isEnabled())) {
      providerIds.add(LDAP_PROVIDER_ID);
      providers.add(ldapAuthenticationProvider());
    }
    if (providerProperties.getCas().isEnabled()) {
      providerIds.add(CAS_PROVIDER_ID);
      providers.add(casAuthenticationProvider());
    }
    if (providerProperties.getRegistered().isEnabled()) {
      providerIds.add(INTERNAL_PROVIDER_ID);
      providers.add(daoAuthenticationProvider());
    }
    boolean ssoProvider = false;
    if (providerProperties.getSaml().isEnabled()) {
      ssoProvider = true;
      providerIds.add(SAML_PROVIDER_ID);
    }
    if (providerProperties.getOidc().stream().anyMatch(p -> p.isEnabled())) {
      ssoProvider = true;
      providerIds.add(OIDC_PROVIDER_ID);
    }
    if (ssoProvider) {
      providers.add(ssoAuthenticationProvider);
    }
    logger.info("Enabled authentication providers: {}", providerIds);

    return new ProviderManager(providers);
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
      response.setContentType(MediaType.APPLICATION_JSON.toString());
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
      response.setContentType(MediaType.APPLICATION_JSON.toString());
      response.getWriter().write(jackson2HttpMessageConverter.getObjectMapper().writeValueAsString(
          controllerExceptionHelper.handleException(accessDeniedException, Level.DEBUG)));
    };
  }

  @Bean
  LoginAuthenticationSucessHandler successHandler() {
    return new LoginAuthenticationSucessHandler(systemProperties, servletContext);
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
    authProvider.setUserDetailsService(registeredUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());

    return authProvider;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
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
    contextSource.setBaseEnvironmentProperties(Collections.singletonMap(
        "com.sun.jndi.ldap.connect.timeout", String.valueOf(ldapProperties.getConnectTimeout())));
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

  @Bean
  @ConditionalOnProperty(
      name = "ldap[0].enabled",
      prefix = AuthenticationProviderProperties.PREFIX,
      havingValue = "true")
  public LdapTemplate ldapTemplate() {
    return new LdapTemplate(ldapContextSource());
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

  // SAML Authentication Configuration

  @Bean
  @ConditionalOnProperty(
      name = "saml.enabled",
      prefix = AuthenticationProviderProperties.PREFIX,
      havingValue = "true")
  public Config samlConfig() {
    return new Config(rootUrl + apiPath + SAML_CALLBACK_PATH, saml2Client());
  }

  @Bean
  @ConditionalOnProperty(
      name = "saml.enabled",
      prefix = AuthenticationProviderProperties.PREFIX,
      havingValue = "true")
  public SsoCallbackFilter samlCallbackFilter() throws Exception {
    final SsoCallbackFilter callbackFilter = new SsoCallbackFilter(samlConfig(), SAML_CALLBACK_PATH);
    callbackFilter.setAuthenticationManager(authenticationManager());
    callbackFilter.setAuthenticationSuccessHandler(successHandler());
    callbackFilter.setAuthenticationFailureHandler(failureHandler());

    return callbackFilter;
  }

  @Bean
  @ConditionalOnProperty(
      name = "saml.enabled",
      prefix = AuthenticationProviderProperties.PREFIX,
      havingValue = "true")
  public SAML2Client saml2Client() {
    final AuthenticationProviderProperties.Saml samlProperties = providerProperties.getSaml();
    final Pattern pathPattern = Pattern.compile("[a-z]+:.*");
    final String idpMetadataPath = pathPattern.matcher(samlProperties.getIdp().getMetaFile()).matches()
        ? samlProperties.getIdp().getMetaFile()
        : "file:" + samlProperties.getIdp().getMetaFile();
    final SAML2Configuration config = new SAML2Configuration(
        "file:" + samlProperties.getKeystore().getFile(),
        samlProperties.getKeystore().getStorePassword(),
        samlProperties.getKeystore().getKeyPassword(),
        idpMetadataPath);
    config.setKeystoreAlias(samlProperties.getKeystore().getKeyAlias());
    if (!samlProperties.getSp().getMetaFile().isEmpty()) {
      config.setServiceProviderMetadataPath("file:" + samlProperties.getSp().getMetaFile());
    }
    if (!samlProperties.getSp().getEntityId().isEmpty()) {
      config.setServiceProviderEntityId(samlProperties.getSp().getEntityId());
    }
    if (!samlProperties.getIdp().getEntityId().isEmpty()) {
      config.setIdentityProviderEntityId(samlProperties.getIdp().getEntityId());
    }
    final String acsUrl = rootUrl + apiPath + AUTH_CALLBACK_PATH;
    if (samlProperties.getAssertionConsumerServiceIndex() == -1) {
      config.setAssertionConsumerServiceUrl(acsUrl);
    }
    config.setAssertionConsumerServiceIndex(samlProperties.getAssertionConsumerServiceIndex());
    config.setMaximumAuthenticationLifetime(samlProperties.getMaxAuthenticationLifetime());
    config.setAuthnRequestBindingType(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
    config.setAuthnRequestSigned(true);
    final SAML2Client client = new SAML2Client(config);
    client.setName(SAML_PROVIDER_ID);
    client.setCallbackUrl(acsUrl);
    client.setCallbackUrlResolver(pathParameterCallbackUrlResolver());

    /* Initialize the client manually for the metadata endpoint */
    client.init();

    return client;
  }

  // OAuth Authentication Configuration

  @Bean
  public Config oauthConfig() {
    final List<Client> clients = new ArrayList<>();
    if (providerProperties.getOidc().stream().anyMatch(p -> p.isEnabled())) {
      clients.add(oidcClient());
    }

    return new Config(rootUrl + apiPath + OAUTH_CALLBACK_PATH, clients);
  }

  @Bean
  public SsoCallbackFilter oauthCallbackFilter() throws Exception {
    final SsoCallbackFilter callbackFilter = new SsoCallbackFilter(oauthConfig(), OAUTH_CALLBACK_PATH + "/**");
    callbackFilter.setAuthenticationManager(authenticationManager());
    callbackFilter.setAuthenticationSuccessHandler(successHandler());
    callbackFilter.setAuthenticationFailureHandler(failureHandler());

    return callbackFilter;
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
}
