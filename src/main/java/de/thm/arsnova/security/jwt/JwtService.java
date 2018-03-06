package de.thm.arsnova.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.security.User;
import de.thm.arsnova.services.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtService {
	private static final String CONFIG_PREFIX = "security.jwt.";
	private static final String ROLE_PREFIX = "ROLE_";
	private static final String ROLES_CLAIM_NAME = "roles";
	private Algorithm algorithm;
	private String serverId;
	private TemporalAmount defaultValidityPeriod;
	private TemporalAmount guestValidityPeriod;
	private JWTVerifier verifier;
	private UserService userService;

	public JwtService(
			final UserService userService,
			@Value("${" + CONFIG_PREFIX + "secret}") final String secret,
			@Value("${" + CONFIG_PREFIX + "serverId}") final String serverId,
			@Value("${" + CONFIG_PREFIX + "validity-period}") final String defaultValidityPeriod)
			throws UnsupportedEncodingException {
		this.userService = userService;
		this.serverId = serverId;
		try {
			this.defaultValidityPeriod = Duration.parse("P" + defaultValidityPeriod);
		} catch (Exception e) {
			throw new IllegalArgumentException(defaultValidityPeriod, e);
		}
		guestValidityPeriod = Duration.parse("P180D");
		algorithm = Algorithm.HMAC256(secret);
		verifier = JWT.require(algorithm)
				.withAudience(serverId)
				.build();
	}

	public String createSignedToken(final User user) {
		String[] roles = user.getAuthorities().stream()
				.map(ga -> ga.getAuthority())
				.filter(ga -> ga.startsWith(ROLE_PREFIX))
				.map(ga -> ga.substring(ROLE_PREFIX.length())).toArray(String[]::new);
		final TemporalAmount expiresAt = user.getAuthProvider() == UserProfile.AuthProvider.ARSNOVA_GUEST
				? guestValidityPeriod : defaultValidityPeriod;
		return JWT.create()
				.withIssuer(serverId)
				.withAudience(serverId)
				.withIssuedAt(new Date())
				.withExpiresAt(Date.from(LocalDateTime.now().plus(expiresAt).toInstant(ZoneOffset.UTC)))
				.withSubject(user.getId())
				.withArrayClaim(ROLES_CLAIM_NAME, roles)
				.sign(algorithm);
	}

	public User verifyToken(final String token) {
		final DecodedJWT decodedJwt = verifier.verify(token);
		final String userId = decodedJwt.getSubject();
		final Collection<GrantedAuthority> authorities = decodedJwt.getClaim(ROLES_CLAIM_NAME).asList(String.class).stream()
				.map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role)).collect(Collectors.toList());

		return userService.loadUser(userId, authorities);
	}
}
