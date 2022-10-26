/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.websocket;

import com.corundumstudio.socketio.SocketIOClient;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import de.thm.arsnova.security.User;
import de.thm.arsnova.service.UserService;

/**
 * This aspect populates the SecurityContextHolder of Spring Security when data are received via WebSockets.
 * It allows WebSocket listeners to access service methods which are secured by Spring Security annotations.
 *
 * @author Daniel Gerhardt
 */
@Aspect
@Configurable
public class WebsocketAuthenticationAspect {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketAuthenticationAspect.class);
	private static final GrantedAuthority WEBSOCKET_AUTHORITY = new SimpleGrantedAuthority("ROLE_WEBSOCKET_ACCESS");

	private UserService userService;

	@Around("execution(void com.corundumstudio.socketio.listener.DataListener+.onData(..)) && args(client, message, ..)")
	public <T> void handleWebsocketAuthentication(final ProceedingJoinPoint pjp,
			final SocketIOClient client, final T message) throws Throwable {
		logger.debug("Executing WebsocketAuthenticationAspect for onData event: Session Id: {}, Message Class: {}",
				client.getSessionId(), message.getClass());
		try {
			populateSecurityContext(client.getSessionId());
			pjp.proceed();
		} finally {
			clearSecurityContext();
		}
	}

	private void populateSecurityContext(final UUID socketId) {
		final String userId = userService.getUserIdToSocketId(socketId);
		if (userId == null) {
			throw new AccessDeniedException("No user authenticated for WebSocket connection");
		}
		final SecurityContext context = SecurityContextHolder.getContext();
		final Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(WEBSOCKET_AUTHORITY);
		final User user = userService.loadUser(userId, authorities);
		final Authentication auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);
	}

	private void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Autowired
	public void setUserService(final UserService userService) {
		this.userService = userService;
	}
}
