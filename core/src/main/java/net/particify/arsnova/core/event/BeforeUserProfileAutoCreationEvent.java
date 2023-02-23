package net.particify.arsnova.core.event;

import java.io.Serial;
import java.util.Map;
import org.springframework.context.ApplicationEvent;

import net.particify.arsnova.core.model.UserProfile;

/**
 * This event is used to publish the new {@link UserProfile} and user attributes
 * provided by the authentication provider before the UserProfile is persisted.
 */
public class BeforeUserProfileAutoCreationEvent extends ApplicationEvent {
  @Serial
  private static final long serialVersionUID = 1L;

  private final transient UserProfile userProfile;
  private final Map<String, Object> userAttributes;

  public BeforeUserProfileAutoCreationEvent(
      final Object source,
      final UserProfile userProfile,
      final Map<String, Object> userAttributes) {
    super(source);
    this.userProfile = userProfile;
    this.userAttributes = userAttributes;
  }

  /**
   * Returns the {@link UserProfile} which will be created.
   */
  public UserProfile getUserProfile() {
    return userProfile;
  }

  /**
   * Returns the user attributes from the authentication provider.
   */
  public Map<String, Object> getUserAttributes() {
    return userAttributes;
  }
}
