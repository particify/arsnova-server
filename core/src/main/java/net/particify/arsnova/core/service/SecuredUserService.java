package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.UserProfile;

@Service
public class SecuredUserService extends AbstractSecuredEntityServiceImpl<UserProfile>
    implements SecuredService, UserService {
  private final UserService userService;

  public SecuredUserService(final UserService userService) {
    super(UserProfile.class, userService);
    this.userService = userService;
  }

  @Override
  @PreAuthorize("isAuthenticated")
  public boolean isAdmin(final String loginId, final UserProfile.AuthProvider authProvider) {
    return userService.isAdmin(loginId, authProvider);
  }

  @Override
  // @PreAuthorize("permitAll")
  public UserProfile getByAuthProviderAndLoginId(final UserProfile.AuthProvider authProvider, final String loginId) {
    return userService.getByAuthProviderAndLoginId(authProvider, loginId);
  }

  @Override
  // @PreAuthorize("permitAll")
  public List<UserProfile> getByLoginId(final String loginId) {
    return userService.getByLoginId(loginId);
  }

  @Override
  // @PreAuthorize("permitAll")
  public UserProfile getByUsername(final String username) {
    return userService.getByUsername(username);
  }

  @Override
  // @PreAuthorize("permitAll")
  public UserProfile create(final String username, final String password) {
    return userService.create(username, password);
  }

  @Override
  @PreAuthorize("denyAll")
  public UserProfile createAnonymizedGuestUser() {
    return userService.createAnonymizedGuestUser();
  }

  @Override
  @Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
  public boolean activateAccount(final String id, final String key, final String clientAddress) {
    return userService.activateAccount(id, key, clientAddress);
  }

  @Override
  @Secured("ROLE_ADMIN")
  public void activateAccount(final String id) {
    userService.activateAccount(id);
  }

  @Override
  @Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
  public void initiatePasswordReset(final String id) {
    userService.initiatePasswordReset(id);
  }

  @Override
  @Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
  public boolean resetPassword(final String id, final String key, final String password) {
    return userService.resetPassword(id, key, password);
  }

  @Override
  @Secured({"ROLE_ANONYMOUS", "ROLE_USER", "RUN_AS_ACCOUNT_MANAGEMENT"})
  public UserProfile resetActivation(final String id, final String clientAddress) {
    return userService.resetActivation(id, clientAddress);
  }

  @Override
  @PreAuthorize("denyAll")
  public String generateGuestId() {
    return userService.generateGuestId();
  }
}
