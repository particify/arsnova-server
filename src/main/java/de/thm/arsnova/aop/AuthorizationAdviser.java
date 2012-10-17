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
	
	@Before("execution(public * de.thm.arsnova.services.*.*(..)) && @annotation(authenticated) && this(object)")
	public void checkAuthorization(Authenticated authenticated, Object object) {
		User u = userService.getUser(SecurityContextHolder.getContext().getAuthentication());
		if (u == null) throw new UnauthorizedException();
	}
}
