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
package de.thm.arsnova.controller;

import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.NotImplementedException;
import de.thm.arsnova.exceptions.PayloadTooLargeException;
import de.thm.arsnova.exceptions.PreconditionFailedException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates security/authentication related exceptions into HTTP status codes.
 */
@ControllerAdvice
public class SecurityExceptionControllerAdvice {

	@ExceptionHandler
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Map<String, String> defaultExceptionHandler(
			final Exception e,
			final HttpServletRequest req
			) {
		final Map<String, String> result = new HashMap<String, String>();
		result.put("code", "500");
		result.put("status", "Internal server error");
		result.put("message", e.getMessage());
		return result;
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(NotFoundException.class)
	public void handleNotFoundException(final Exception e, final HttpServletRequest request) {
	}

	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ExceptionHandler(UnauthorizedException.class)
	public void handleUnauthorizedException(final Exception e, final HttpServletRequest request) {
	}

	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
	public void handleAuthenticationCredentialsNotFoundException(final Exception e, final HttpServletRequest request) {
	}

	@ExceptionHandler(AccessDeniedException.class)
	public void handleAccessDeniedException(
			final Exception e,
			final HttpServletRequest request,
			final HttpServletResponse response
			) {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
				authentication == null
				|| authentication.getPrincipal() == null
				|| authentication instanceof AnonymousAuthenticationToken
				) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			return;
		}
		response.setStatus(HttpStatus.FORBIDDEN.value());
	}

	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ExceptionHandler(ForbiddenException.class)
	public void handleForbiddenException(final Exception e, final HttpServletRequest request) {
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ExceptionHandler(NoContentException.class)
	public void handleNoContentException(final Exception e, final HttpServletRequest request) {
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(BadRequestException.class)
	public void handleBadRequestException(final Exception e, final HttpServletRequest request) {
	}

	@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
	@ExceptionHandler(PreconditionFailedException.class)
	public void handlePreconditionFailedException(final Exception e, final HttpServletRequest request) {
	}

	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	@ExceptionHandler(NotImplementedException.class)
	public void handleNotImplementedException(final Exception e, final HttpServletRequest request) {
	}

	@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
	@ExceptionHandler(PayloadTooLargeException.class)
	public void handlePayloadTooLargeException(final Exception e, final HttpServletRequest request) {
	}
}
