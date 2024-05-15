package net.particify.arsnova.core.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Component;

import net.particify.arsnova.core.config.properties.SecurityProperties;
import net.particify.arsnova.core.model.ClientAuthentication;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.security.jwt.JwtService;
import net.particify.arsnova.core.security.jwt.JwtToken;
import net.particify.arsnova.core.web.exceptions.BadRequestException;

@Component
public class AuthenticationService {
  private static final int LOGIN_TRY_RESET_DELAY_MS = 30 * 1000;
  private static final int LOGIN_BAN_RESET_DELAY_MS = 2 * 60 * 1000;

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

  /* for STOMP over ws functionality */
  private final ConcurrentHashMap<String, String> wsSessionIdToJwt = new ConcurrentHashMap<>();

  private ConcurrentHashMap<String, Byte> loginTries;
  private Set<String> loginBans;
  private SecurityProperties securityProperties;
  private JwtService jwtService;

  @Autowired(required = false)
  private GuestUserDetailsService guestUserDetailsService;

  @Autowired(required = false)
  private DaoAuthenticationProvider daoProvider;

  @Autowired(required = false)
  private LdapAuthenticationProvider ldapAuthenticationProvider;

  public AuthenticationService(final SecurityProperties securityProperties, final JwtService jwtService) {
    this.securityProperties = securityProperties;
    this.jwtService = jwtService;
  }

  {
    loginTries = new ConcurrentHashMap<>();
    loginBans = Collections.synchronizedSet(new HashSet<String>());
  }

  @Scheduled(fixedDelay = LOGIN_TRY_RESET_DELAY_MS)
  public void resetLoginTries() {
    if (!loginTries.isEmpty()) {
      logger.debug("Resetting counters for failed logins.");
      loginTries.clear();
    }
  }

  @Scheduled(fixedDelay = LOGIN_BAN_RESET_DELAY_MS)
  public void resetLoginBans() {
    if (!loginBans.isEmpty()) {
      logger.info("Clearing temporary bans for failed logins ({}).", loginBans.size());
      loginBans.clear();
    }
  }

  public User getCurrentUser() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
      return null;
    }

    return (User) authentication.getPrincipal();
  }

  public net.particify.arsnova.core.model.ClientAuthentication getCurrentClientAuthentication(final boolean refresh) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
      return null;
    }
    final User user = (User) authentication.getPrincipal();
    final String jwt = !refresh && authentication instanceof JwtToken
        ? (String) authentication.getCredentials() : jwtService.createSignedToken(user, false);

    final ClientAuthentication clientAuthentication = new ClientAuthentication(
        user.getId(),
        user.getDisplayId(),
        user.getDisplayName(),
        user.getUsername(),
        user.getAuthProvider(),
        jwt);

    return clientAuthentication;
  }

  public void authenticate(final UsernamePasswordAuthenticationToken token,
      final UserProfile.AuthProvider authProvider, final String clientAddress) {
    if (isBannedFromLogin(clientAddress)) {
      throw new BadRequestException();
    }

    final Authentication auth;
    switch (authProvider) {
      case LDAP:
        auth = ldapAuthenticationProvider.authenticate(token);
        break;
      case ARSNOVA:
        auth = daoProvider.authenticate(token);
        break;
      case ARSNOVA_GUEST:
        final String id = token.getName();
        boolean autoCreate = false;
        if (id == null || id.isEmpty()) {
          autoCreate = true;
        }
        final UserDetails userDetails = guestUserDetailsService.loadUserByUsername(id, autoCreate);
        if (userDetails == null) {
          throw new UsernameNotFoundException("Guest user does not exist");
        }
        auth = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());

        break;
      default:
        throw new IllegalArgumentException("Unsupported authentication provider");
    }

    if (!auth.isAuthenticated()) {
      increaseFailedLoginCount(clientAddress);
      throw new BadRequestException();
    }
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private void increaseFailedLoginCount(final String addr) {
    Byte tries = loginTries.get(addr);
    if (null == tries) {
      tries = 0;
    }
    if (tries < securityProperties.getLoginTryLimit()) {
      loginTries.put(addr, ++tries);
      if (securityProperties.getLoginTryLimit() == tries) {
        logger.info("Temporarily banned {} from login.", addr);
        loginBans.add(addr);
      }
    }
  }

  private boolean isBannedFromLogin(final String addr) {
    return loginBans.contains(addr);
  }
}
