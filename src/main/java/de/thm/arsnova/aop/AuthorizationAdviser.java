package de.thm.arsnova.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;

import de.thm.arsnova.annotation.Authenticated;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.IUserService;

@Aspect
public class AuthorizationAdviser {

	static IUserService userService;
	
	public void setUserService(IUserService uService) {
		userService = uService;
	}
	
	/** This method checks if the user has a valid authorization from security context 
	 * 
	 * @param authenticated
	 * @param object
	 */
	@Before("execution(public * de.thm.arsnova.services.*.*(..)) && @annotation(authenticated) && this(object)")
	public void checkAuthorization(Authenticated authenticated, Object object) {
		User u = userService.getUser(SecurityContextHolder.getContext().getAuthentication());
		if (u == null) throw new UnauthorizedException();
		// TODO: For unauthorized users e.g. after logout there is still a user object with username 'anonymous'
		if (u.getUsername().equals("anonymous")) throw new UnauthorizedException();
	}
	
	/** This method checks if the user is enlisted in current ARSnova session
	 *
	 * @param authenticated
	 * @param object
	 */
	@Before("execution(public * de.thm.arsnova.services.*.*(..)) && @annotation(authenticated) && this(object)")
	public void checkSessionMembership(Authenticated authenticated, Object object) {
		//TODO: Implement check based on session membership lists
	}
}
