package de.thm.arsnova.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import de.thm.arsnova.annotation.Authenticated;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.IUserService;

@Aspect
public class AuthorizationAdviser {

	private IUserService userService;

	public final void setUserService(final IUserService uService) {
		userService = uService;
	}

	/**
	 * This method checks if the user has a valid authorization from security
	 * context
	 *
	 * @param authenticated
	 * @param object
	 */
	@Before("execution(public * de.thm.arsnova.services.*.*(..)) && @annotation(authenticated) && this(object)")
	public final void checkAuthorization(final Authenticated authenticated, final Object object) {
		User u = userService.getCurrentUser();
		if (u == null) {
			throw new UnauthorizedException();
		}
		if (u.getUsername().equals("anonymous")) {
			throw new UnauthorizedException();
		}
	}

	/**
	 * This method checks if the user is enlisted in current ARSnova session
	 *
	 * @param authenticated
	 * @param object
	 */
	@Before("execution(public * de.thm.arsnova.services.*.*(..)) && @annotation(authenticated) && this(object)")
	public final void checkSessionMembership(final Authenticated authenticated, final Object object) {
		/// TODO Implement check based on session membership lists
	}
}
