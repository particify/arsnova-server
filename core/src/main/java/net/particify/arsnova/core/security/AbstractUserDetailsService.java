package net.particify.arsnova.core.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    final UserProfile userProfile = getUserProfile(loginId, authProvider).orElseGet(() -> {
      final UserProfile newUserProfile = new UserProfile(authProvider, loginId);
      newUserProfile.setPerson(buildPersonFromAttributes(userAttributes));
      applicationEventPublisher.publishEvent(
          new BeforeUserProfileAutoCreationEvent(this, newUserProfile, userAttributes));
      return userService.create(newUserProfile);
    });
    if (personNeedsUpdate(userProfile.getPerson(), userAttributes)) {
      userProfile.setPerson(buildPersonFromAttributes(userAttributes));
      userService.update(userProfile);
    }

    return new User(
      userProfile,
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

  protected UserProfile.Person buildPersonFromAttributes(final Map<String, Object> attributes) {
    return new UserProfile.Person();
  }

  protected String extractAttribute(final Map<String, Object> attributes, final String oid) {
    final Object attribute = attributes.get(oid);
    if (attribute == null) {
      return "";
    }
    if (attribute instanceof List<?> list) {
      return list.get(0).toString();
    } else {
      return attribute.toString();
    }
  }

  private boolean personNeedsUpdate(final UserProfile.Person person, final Map<String, Object> attributes) {
    final UserProfile.Person newPerson = buildPersonFromAttributes(attributes);
    return !Objects.equals(newPerson.getMail(), person.getMail())
        || !Objects.equals(newPerson.getFirstName(), person.getFirstName())
        || !Objects.equals(newPerson.getLastName(), person.getLastName())
        || !Objects.equals(newPerson.getDisplayName(), person.getDisplayName());
  }
}
