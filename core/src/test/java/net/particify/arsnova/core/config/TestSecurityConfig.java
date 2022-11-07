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

import jakarta.servlet.ServletContext;
import org.pac4j.core.config.Config;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import net.particify.arsnova.core.config.properties.AuthenticationProviderProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;

@TestConfiguration
@EnableGlobalMethodSecurity(mode = AdviceMode.ASPECTJ, prePostEnabled = true)
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig extends SecurityConfig {
  public TestSecurityConfig(
      final SystemProperties systemProperties,
      final AuthenticationProviderProperties authenticationProviderProperties,
      final ServletContext servletContext) {
    super(systemProperties, authenticationProviderProperties, servletContext);
  }

  @Override
  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }


  /* Override for test unnecessary Beans with null */

  @MockBean
  public DaoAuthenticationProvider daoAuthenticationProvider;

  @MockBean
  public FacebookClient facebookClient;

  @MockBean
  public GoogleOidcClient googleClient;

  @MockBean
  public TwitterClient twitterClient;

  @MockBean
  @Qualifier("oauthConfig")
  public Config oauthConfig;

  @MockBean
  @Qualifier("samlConfig")
  public Config samlConfig;
}
