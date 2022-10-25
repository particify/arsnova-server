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

package net.particify.arsnova.core.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

public class CorsFilter extends org.springframework.web.filter.CorsFilter {
  private static final String X_REQUESTED_WITH = "X-Requested-With";
  private static final String TOKEN_HEADER_NAME = "Arsnova-Auth-Token";
  private final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

  public CorsFilter(final List<String> origins) {
    super(configurationSource(origins));
    logger.info("CorsFilter initialized. Allowed origins: {}", origins);
  }

  private static UrlBasedCorsConfigurationSource configurationSource(final List<String> origins) {
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    final CorsConfiguration config;

    if (!origins.isEmpty()) {
      /* Grant full access from specified origins */
      config = new CorsConfiguration();
      config.setAllowedOrigins(origins);
      config.addAllowedHeader(HttpHeaders.ACCEPT);
      config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
      config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
      config.addAllowedHeader(X_REQUESTED_WITH);
      config.addAllowedMethod(HttpMethod.GET);
      config.addAllowedMethod(HttpMethod.POST);
      config.addAllowedMethod(HttpMethod.PUT);
      config.addAllowedMethod(HttpMethod.PATCH);
      config.addAllowedMethod(HttpMethod.DELETE);
      config.setAllowCredentials(true);
      source.registerCorsConfiguration("/**", config);
    } else {
      /* Grant limited access from all origins */
      config = new CorsConfiguration();
      config.addAllowedOrigin("*");
      config.addAllowedHeader(HttpHeaders.ACCEPT);
      config.addAllowedHeader(X_REQUESTED_WITH);
      config.addAllowedMethod(HttpMethod.GET);
      source.registerCorsConfiguration("/", config);
      source.registerCorsConfiguration("/arsnova-config", config);
      source.registerCorsConfiguration("/configuration/", config);
      source.registerCorsConfiguration("/statistics", config);
    }

    return source;
  }
}
