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

package de.thm.arsnova.controller;

import java.util.Map;
import javax.naming.OperationNotSupportedException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import de.thm.arsnova.model.EntityValidationException;
import de.thm.arsnova.service.exceptions.UserAlreadyExistsException;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.ForbiddenException;
import de.thm.arsnova.web.exceptions.NoContentException;
import de.thm.arsnova.web.exceptions.NotFoundException;
import de.thm.arsnova.web.exceptions.NotImplementedException;
import de.thm.arsnova.web.exceptions.PayloadTooLargeException;
import de.thm.arsnova.web.exceptions.PreconditionFailedException;
import de.thm.arsnova.web.exceptions.UnauthorizedException;

/**
 * Translates exceptions into HTTP status codes.
 */
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
	private ControllerExceptionHelper helper;

	public ControllerExceptionHandler(final ControllerExceptionHelper helper) {
		this.helper = helper;
	}

	@ExceptionHandler(NoContentException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Map<String, Object> handleNoContentException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.TRACE);
	}

	@ExceptionHandler(NotFoundException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, Object> handleNotFoundException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.TRACE);
	}

	@ExceptionHandler(UnauthorizedException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Map<String, Object> handleUnauthorizedException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.TRACE);
	}

	@ExceptionHandler(AuthenticationException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ResponseBody
	public Map<String, Object> handleAuthenticationExceptionException(
			final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler(AccessDeniedException.class)
	@ResponseBody
	public Map<String, Object> handleAccessDeniedException(
			final Exception e,
			final HttpServletRequest request,
			final HttpServletResponse response) {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| authentication.getPrincipal() == null
				|| authentication instanceof AnonymousAuthenticationToken) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
		} else {
			response.setStatus(HttpStatus.FORBIDDEN.value());
		}

		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler(ForbiddenException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Map<String, Object> handleForbiddenException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler(BadRequestException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> handleBadRequestException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler(EntityValidationException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> handleEntityValidationException(
			final EntityValidationException e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler(UserAlreadyExistsException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> handleUserAlreadyExistsException(
			final UserAlreadyExistsException e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler(PreconditionFailedException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
	public Map<String, Object> handlePreconditionFailedException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler({NotImplementedException.class, OperationNotSupportedException.class})
	@ResponseBody
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	public Map<String, Object> handleNotImplementedException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	@ExceptionHandler(PayloadTooLargeException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
	public Map<String, Object> handlePayloadTooLargeException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.DEBUG);
	}

	/* FIXME: Wrap persistance Exceptions - do not handle persistance Exceptions at the controller layer */
	@ExceptionHandler(DocumentNotFoundException.class)
	@ResponseBody
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, Object> handleDocumentNotFoundException(final Exception e, final HttpServletRequest request) {
		return helper.handleException(e, Level.TRACE);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(final Exception ex, final Object body,
			final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
		return new ResponseEntity<>(helper.handleException(ex, Level.TRACE), headers, status);
	}
}
