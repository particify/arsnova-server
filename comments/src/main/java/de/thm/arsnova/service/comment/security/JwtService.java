package de.thm.arsnova.service.comment.security;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Collection;
import java.util.stream.Collectors;

import de.thm.arsnova.service.comment.config.properties.SecurityProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final String ROLE_PREFIX = "ROLE_";
  private static final String ROLES_CLAIM_NAME = "roles";

  private Algorithm algorithm;
  private JWTVerifier verifier;
  private String jwtSecret;

  public JwtService(final SecurityProperties securityProperties) {
    jwtSecret = securityProperties.getJwt().getSecret();
    algorithm = Algorithm.HMAC256(jwtSecret);
    verifier = JWT.require(algorithm)
        .build();
  }

  public AuthenticatedUser verifyToken(final String token) {
    final DecodedJWT decodedJwt = verifier.verify(token);
    String userId = decodedJwt.getSubject();

    final Collection<GrantedAuthority> authorities = decodedJwt.getClaim(ROLES_CLAIM_NAME).asList(String.class).stream()
        .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role)).collect(Collectors.toList());

    return new AuthenticatedUser(userId, authorities, token);
  }
}
