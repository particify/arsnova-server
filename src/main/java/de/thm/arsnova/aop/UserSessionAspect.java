package de.thm.arsnova.aop;

import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.events.Publisher;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.UserSessionService;

@Aspect
public class UserSessionAspect {

	@Autowired
	private UserSessionService userSessionService;

	@Autowired
	private IUserService userService;

	@Autowired
	private Publisher publisher;

	/** Sets current user and ARSnova session in session scoped UserSessionService 
	 * 
	 * @param jp
	 * @param keyword
	 * @param session
	 */
	@AfterReturning(
		pointcut="execution(public * de.thm.arsnova.services.SessionService.joinSession(..)) && args(keyword)",
		returning="session"
	)
	public final void joinSessionAdvice(final JoinPoint jp, final String keyword, final Session session) {
		userSessionService.setUser(userService.getCurrentUser());
		userSessionService.setSession(session);
	}

	/** Sets current user, ARSnova session and websocket session ID in session scoped UserSessionService 
	 * 
	 * @param jp
	 * @param keyword
	 * @param socketId
	 * @param session
	 */
	@AfterReturning(
		pointcut="execution(public * de.thm.arsnova.services.SessionService.joinSession(..)) && args(keyword, socketId)",
		returning="session"
	)
	public final void joinSessionAdviceWithWebsocket(final JoinPoint jp, final String keyword, final UUID socketId, final Session session) {
		userSessionService.setSession(session);
		userSessionService.setSocketId(socketId);
	}
	
	/** Sets current user, ARSnova session and websocket session ID in session scoped UserSessionService 
	 * 
	 * @param jp
	 * @param keyword
	 * @param socketId
	 * @param session
	 */
	@AfterReturning(
		pointcut="execution(public * de.thm.arsnova.controller.LoginController.doLogin(..)) " +
				"&& args(type, guestName, forcedReferer, successUrl, failureUrl, role, ..)"
	)
	public final void doLogin(
		final JoinPoint jp,
		final String type,
		final String guestName,
		final String forcedReferer,
		final String successUrl,
		final String failureUrl,
		final UserSessionService.Role role) {
		
		userSessionService.setRole(role);
		userSessionService.setUser(userService.getCurrentUser());
	}
}
