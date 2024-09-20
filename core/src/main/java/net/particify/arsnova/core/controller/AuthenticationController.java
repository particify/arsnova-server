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

package net.particify.arsnova.core.controller;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.util.FindBest;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStoreFactory;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import net.particify.arsnova.core.config.SecurityConfig;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.ClientAuthentication;
import net.particify.arsnova.core.model.LoginCredentials;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.security.AuthenticationService;
import net.particify.arsnova.core.security.LoginAuthenticationSucessHandler;
import net.particify.arsnova.core.service.UserService;
import net.particify.arsnova.core.web.exceptions.NotImplementedException;

@RestController
@EntityRequestMapping("/auth")
public class AuthenticationController {
  private AuthenticationService authenticationService;
  private UserService userService;
  private OidcClient oidcClient;
  private SAML2Client saml2Client;
  private CasAuthenticationEntryPoint casEntryPoint;
  private Config oauthConfig;
  private Config samlConfig;
  private String apiPath;

  public AuthenticationController(
      final AuthenticationService authenticationService,
      @Qualifier("securedUserService") final UserService userService,
      final SystemProperties systemProperties,
      final ServletContext servletContext,
      @Qualifier("oauthConfig") final Config oauthConfig,
      @Autowired(required = false) @Qualifier("samlConfig") final Config samlConfig) {
    this.authenticationService = authenticationService;
    this.userService = userService;
    this.oauthConfig = oauthConfig;
    this.samlConfig = samlConfig;
    final String proxyPath = systemProperties.getApi().getProxyPath();
    this.apiPath = proxyPath != null && !proxyPath.isEmpty() ? proxyPath : servletContext.getContextPath();
  }

  @Autowired(required = false)
  public void setOidcClient(final OidcClient oidcClient) {
    this.oidcClient = oidcClient;
  }

  @Autowired(required = false)
  public void setSaml2Client(final SAML2Client saml2Client) {
    this.saml2Client = saml2Client;
  }

  @Autowired(required = false)
  public void setCasEntryPoint(final CasAuthenticationEntryPoint casEntryPoint) {
    this.casEntryPoint = casEntryPoint;
  }

  @PostMapping("/login")
  public ClientAuthentication login(@RequestParam(defaultValue = "false") final boolean refresh,
      final HttpServletRequest request, final HttpServletResponse response) {
    if (request.getCookies() != null && Arrays.stream(request.getCookies())
        .anyMatch(c -> c.getName().equalsIgnoreCase(LoginAuthenticationSucessHandler.AUTH_COOKIE_NAME))) {
      /* Delete cookie */
      final Cookie cookie = new Cookie(LoginAuthenticationSucessHandler.AUTH_COOKIE_NAME, null);
      cookie.setPath(apiPath);
      cookie.setMaxAge(0);
      response.addCookie(cookie);
    }

    final ClientAuthentication authentication = authenticationService.getCurrentClientAuthentication(refresh);
    if (authentication == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    } else {
      updateLastActivityTimestamp(authentication);
    }
    return authentication;
  }

  @PostMapping("/login/guest")
  public ClientAuthentication loginGuest(
      final HttpServletRequest request,
      @RequestBody(required = false) final LoginCredentials loginCredentials) {
    final String guestId = loginCredentials != null ? loginCredentials.getLoginId() : null;
    if (guestId == null) {
      final ClientAuthentication currentAuthentication = authenticationService.getCurrentClientAuthentication(false);
      if (currentAuthentication != null
          && currentAuthentication.getAuthProvider() == UserProfile.AuthProvider.ARSNOVA_GUEST) {
        return currentAuthentication;
      }
    }
    authenticationService.authenticate(new UsernamePasswordAuthenticationToken(guestId, null),
        UserProfile.AuthProvider.ARSNOVA_GUEST, request.getRemoteAddr());

    return authenticationService.getCurrentClientAuthentication(false);
  }

  @PostMapping("/login/{providerId}")
  public ClientAuthentication loginViaProvider(
      @PathVariable final String providerId,
      @RequestBody final LoginCredentials loginCredentials,
      final HttpServletRequest request) {
    switch (providerId) {
      case "registered":
        final String loginId = loginCredentials.getLoginId().toLowerCase();
        authenticationService.authenticate(new UsernamePasswordAuthenticationToken(
            loginId, loginCredentials.getPassword()),
            UserProfile.AuthProvider.ARSNOVA, request.getRemoteAddr());

        return authenticationService.getCurrentClientAuthentication(false);
      case SecurityConfig.LDAP_PROVIDER_ID:
        authenticationService.authenticate(new UsernamePasswordAuthenticationToken(
            loginCredentials.getLoginId(), loginCredentials.getPassword()),
            UserProfile.AuthProvider.LDAP, request.getRemoteAddr());

        return authenticationService.getCurrentClientAuthentication(false);
      default:
        throw new IllegalArgumentException("Invalid provider ID.");
    }
  }

  @GetMapping("/sso/{providerId}")
  public View redirectToSso(@PathVariable final String providerId,
      final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    switch (providerId) {
      case SecurityConfig.OIDC_PROVIDER_ID:
        if (oidcClient == null) {
          throw new IllegalArgumentException("Invalid provider ID.");
        }
        return buildSsoRedirectView(oidcClient, oauthConfig, request, response);
      case SecurityConfig.SAML_PROVIDER_ID:
        if (saml2Client == null) {
          throw new IllegalArgumentException("Invalid provider ID.");
        }
        return buildSsoRedirectView(saml2Client, samlConfig, request, response);
      case SecurityConfig.CAS_PROVIDER_ID:
        if (casEntryPoint == null) {
          throw new IllegalArgumentException("Invalid provider ID.");
        }
        casEntryPoint.commence(request, response, null);
        return null;
      default:
        throw new IllegalArgumentException("Invalid provider ID.");
    }
  }

  private RedirectView buildSsoRedirectView(
      final IndirectClient client,
      final Config config,
      final HttpServletRequest request,
      final HttpServletResponse response) {
    final JEEContext context = new JEEContext(request, response);
    final SessionStore sessionStore = FindBest.sessionStoreFactory(
        null, config, JEESessionStoreFactory.INSTANCE).newSessionStore();
    final Optional<RedirectView> view = client.getRedirectionAction(context, sessionStore).map(action -> {
      if (action instanceof WithLocationAction) {
        return new RedirectView(((WithLocationAction) action).getLocation());
      }
      return null;
    });
    return view.orElseThrow(() -> {
      throw new IllegalStateException("No URL for redirect found.");
    });
  }

  @GetMapping(value = "/config/saml/sp-metadata.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public String samlSpMetadata() throws IOException {
    if (saml2Client == null) {
      throw new NotImplementedException("SAML authentication is disabled.");
    }

    return saml2Client.getConfiguration().getServiceProviderMetadataResource() != null
        ? saml2Client.getConfiguration().getServiceProviderMetadataResource().getContentAsString(StandardCharsets.UTF_8)
        : saml2Client.getServiceProviderMetadataResolver().getMetadata();
  }

  private void updateLastActivityTimestamp(final ClientAuthentication authentication) {
    final UserProfile userProfile = userService.getByAuthProviderAndLoginId(
        authentication.getAuthProvider(), authentication.getLoginId());
    if (userProfile.getLastActivityTimestamp().toInstant().isBefore(
          Instant.now().minus(1, ChronoUnit.HOURS))) {
      userProfile.setLastActivityTimestamp(new Date());
      userService.update(userProfile);
    }
  }
}
