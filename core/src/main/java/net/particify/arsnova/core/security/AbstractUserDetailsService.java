package net.particify.arsnova.core.security;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import net.particify.arsnova.core.event.BeforeUserProfileAutoCreationEvent;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.service.UserService;

/**
 * This class implements common logic to build
 * {@link org.springframework.security.core.userdetails.UserDetails}
 * from a {@link UserProfile} and trigger the creation of the latter if
 * necessary. Keep in mind that not all UserDetails services extend this class
 * and changes made here might need to be replicated for them.
 */
public abstract class AbstractUserDetailsService implements ApplicationEventPublisherAware {
  protected final UserProfile.AuthProvider defaultAuthProvider;
  protected final UserService userService;
  private ApplicationEventPublisher applicationEventPublisher;

  public AbstractUserDetailsService(
      final UserProfile.AuthProvider defaultAuthProvider,
      final UserService userService) {
    this.defaultAuthProvider = defaultAuthProvider;
    this.userService = userService;
  }

  @Override
  @Autowired
  public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  /**
   * Retrieves a {@link UserProfile} and wraps it in a {@link User} object.
   */
  protected User get(
      final String loginId,
      final Collection<GrantedAuthority> grantedAuthorities) {
    return get(loginId, defaultAuthProvider, grantedAuthorities);
  }

  /**
   * Retrieves a {@link UserProfile} and wraps it in a {@link User} object.
   */
  protected User get(final String loginId,
      final UserProfile.AuthProvider authProvider,
      final Collection<GrantedAuthority> grantedAuthorities) {
    return new User(
      getUserProfile(loginId, authProvider).orElseThrow(
        () -> new UsernameNotFoundException("User does not exist.")),
      grantedAuthorities);
  }

  /**
   * Retrieves a {@link UserProfile} and wraps it in a {@link User} object. If
   * the UserProfile does not exist, it is created.
   */
  protected User getOrCreate(
      final String loginId,
      final Collection<GrantedAuthority> grantedAuthorities,
      final Map<String, Object> userAttributes) {
    return getOrCreate(loginId, defaultAuthProvider, grantedAuthorities, userAttributes);
  }

  /**
   * Retrieves a {@link UserProfile} and wraps it in a {@link User} object. If
   * the UserProfile does not exist, it is created.
   */
  protected User getOrCreate(
      final String loginId,
      final UserProfile.AuthProvider authProvider,
      final Collection<GrantedAuthority> grantedAuthorities,
      final Map<String, Object> userAttributes) {
    return new User(
      getUserProfile(loginId, authProvider).orElseGet(() -> {
        final UserProfile userProfile = new UserProfile(authProvider, loginId);
        applicationEventPublisher.publishEvent(
            new BeforeUserProfileAutoCreationEvent(this, userProfile, userAttributes));
        return userService.create(userProfile);
      }),
      grantedAuthorities);
  }

  private Optional<UserProfile> getUserProfile(
      final String loginId,
      final UserProfile.AuthProvider authProvider) {
    if (authProvider == UserProfile.AuthProvider.NONE) {
      throw new IllegalArgumentException("Invalid auth provider.");
    }

    return Optional.ofNullable(
      userService.getByAuthProviderAndLoginId(authProvider, loginId));
  }
}
