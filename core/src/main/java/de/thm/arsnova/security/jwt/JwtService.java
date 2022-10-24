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

package de.thm.arsnova.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.security.User;
import de.thm.arsnova.service.UserService;

@Service
public class JwtService {
  private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
  private static final String ROLE_PREFIX = "ROLE_";
  private static final String USER_ROLE = "USER";
  private static final String GUEST_ROLE = "GUEST_USER";
  private static final String ROLES_CLAIM_NAME = "roles";
  private Algorithm algorithm;
  private String serverId;
  private TemporalAmount defaultValidityPeriod;
  private TemporalAmount guestValidityPeriod;
  private TemporalAmount temporaryValidityPeriod;
  private JWTVerifier verifier;
  private JWTVerifier legacyVerifier;
  private UserService userService;
  private List<GrantedAuthority> legacyAllowedAuthorities;

  public JwtService(
      final UserService userService,
      final SecurityProperties securityProperties) {
    this.userService = userService;
    this.serverId = securityProperties.getJwt().getServerId();
    this.defaultValidityPeriod = securityProperties.getJwt().getValidityPeriod();
    guestValidityPeriod = Duration.parse("P180D");
    temporaryValidityPeriod = Duration.parse("PT30S");
    algorithm = Algorithm.HMAC256(securityProperties.getJwt().getSecret());
    verifier = JWT.require(algorithm)
        .withAudience(serverId)
        .build();
    /* Legacy token handling */
    if (securityProperties.getJwt().getLegacySecret() != null
        && securityProperties.getJwt().getLegacyServerId() != null) {
      logger.info("JWT settings for legacy tokens active.");
      final Algorithm previousAlgorithm = Algorithm.HMAC256(securityProperties.getJwt().getLegacySecret());
      legacyVerifier = JWT.require(previousAlgorithm)
          .withAudience(securityProperties.getJwt().getLegacyServerId())
          .build();
      legacyAllowedAuthorities = List.of(
          new SimpleGrantedAuthority(ROLE_PREFIX + USER_ROLE),
          new SimpleGrantedAuthority(ROLE_PREFIX + GUEST_ROLE)
      );
    }
  }

  public String createSignedToken(final User user, final boolean temporary) {
    final String[] roles = user.getAuthorities().stream()
        .map(ga -> ga.getAuthority())
        .filter(ga -> ga.startsWith(ROLE_PREFIX))
        .map(ga -> ga.substring(ROLE_PREFIX.length())).toArray(String[]::new);
    final TemporalAmount expiresAt = temporary ? temporaryValidityPeriod
        : (user.getAuthProvider() == UserProfile.AuthProvider.ARSNOVA_GUEST
        ? guestValidityPeriod : defaultValidityPeriod);
    return JWT.create()
        .withIssuer(serverId)
        .withAudience(serverId)
        .withIssuedAt(new Date())
        .withExpiresAt(Date.from(
            LocalDateTime.now().plus(expiresAt).atZone(ZoneId.systemDefault()).toInstant()))
        .withSubject(user.getId())
        .withArrayClaim(ROLES_CLAIM_NAME, roles)
        .sign(algorithm);
  }

  public User verifyToken(final String token) {
    try {
      final DecodedJWT decodedJwt = verifier.verify(token);
      final String userId = decodedJwt.getSubject();
      final Collection<GrantedAuthority> authorities =
          decodedJwt.getClaim(ROLES_CLAIM_NAME).asList(String.class).stream()
          .map(role -> role.replace("-", "__"))
          .map(role -> role.replaceFirst("^CREATOR__", "OWNER__"))
          .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role)).collect(Collectors.toList());

      return userService.loadUser(userId, authorities);
    } catch (final JWTVerificationException e) {
      if (legacyVerifier == null) {
        throw e;
      }
      logger.debug("Token invalid, trying with legacy algorithm settings.");
      final User user = verifyLegacyToken(token);
      if (user == null) {
        throw e;
      }
      return user;
    }
  }

  /**
   * Verifies tokens which were created with a legacy algorithm configuration.
   * For security reasons, only tokens created for guest accounts are allowed
   * and claimed roles are ignored for authorization and instead overridden.
   */
  private User verifyLegacyToken(final String token) {
    final DecodedJWT decodedJwt = legacyVerifier.verify(token);
    logger.debug("Legacy token verified.");
    final String userId = decodedJwt.getSubject();
    final List<String> roleClaims = decodedJwt.getClaim(ROLES_CLAIM_NAME).asList(String.class);
    if (!roleClaims.contains(GUEST_ROLE)) {
      logger.warn("Legacy token for user ID {} valid but not acceptable for non-guest role claims: {}",
          userId, roleClaims);
      return null;
    }

    return userService.loadUser(userId, legacyAllowedAuthorities);
  }
}
