package de.thm.arsnova.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.ClientAuthentication;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.security.User;

@Service
public class SecuredUserService extends AbstractSecuredEntityServiceImpl<UserProfile>
		implements SecuredService, UserService {
	private final UserService userService;

	public SecuredUserService(final UserService userService) {
		super(UserProfile.class, userService);
		this.userService = userService;
	}

	@Override
	@PreAuthorize("denyAll")
	public User getCurrentUser() {
		return userService.getCurrentUser();
	}

	@Override
	// @PreAuthorize("permitAll")
	public ClientAuthentication getCurrentClientAuthentication(final boolean refresh) {
		return userService.getCurrentClientAuthentication(refresh);
	}

	@Override
	@PreAuthorize("isAuthenticated")
	public boolean isAdmin(final String loginId, final UserProfile.AuthProvider authProvider) {
		return userService.isAdmin(loginId, authProvider);
	}

	@Override
	// @PreAuthorize("permitAll")
	public void authenticate(
			final UsernamePasswordAuthenticationToken token,
			final UserProfile.AuthProvider authProvider, final String clientAddress) {
		userService.authenticate(token, authProvider, clientAddress);
	}

	@Override
	@PreAuthorize("denyAll")
	public User loadUser(
			final UserProfile.AuthProvider authProvider,
			final String loginId,
			final Collection<GrantedAuthority> grantedAuthorities,
			final boolean autoCreate) throws UsernameNotFoundException {
		return userService.loadUser(authProvider, loginId, grantedAuthorities, autoCreate);
	}

	@Override
	@PreAuthorize("denyAll")
	public User loadUser(
			final String userId,
			final Collection<GrantedAuthority> grantedAuthorities) {
		return userService.loadUser(userId, grantedAuthorities);
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
	@PreAuthorize("hasPermission(#userProfile, 'read')")
	public Set<UserProfile.RoomHistoryEntry> getRoomHistory(final UserProfile userProfile) {
		return userService.getRoomHistory(userProfile);
	}

	@Override
	@PreAuthorize("hasPermission(#userProfile, 'update')")
	public void addRoomToHistory(final UserProfile userProfile, final Room room) {
		userService.addRoomToHistory(userProfile, room);
	}

	@Override
	@PreAuthorize("hasPermission(#userProfile, 'update')")
	public void deleteRoomFromHistory(final UserProfile userProfile, final Room room) {
		userService.deleteRoomFromHistory(userProfile, room);
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
}
