/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.services.UserSessionService;

@Aspect
public class UserSessionAspect {

	@Autowired
	private UserSessionService userSessionService;

	/** Sets current user and ARSnova session in session scoped UserSessionService
	 *
	 * @param jp
	 * @param keyword
	 * @param session
	 */
	@AfterReturning(
			pointcut = "execution(public * de.thm.arsnova.services.SessionService.joinSession(..)) && args(keyword)",
			returning = "session"
			)
	public final void joinSessionAdvice(final JoinPoint jp, final String keyword, final Session session) {
		userSessionService.setSession(session);
	}
}
