package de.thm.arsnova.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.NotImplementedException;
import de.thm.arsnova.exceptions.PreconditionFailedException;
import de.thm.arsnova.exceptions.UnauthorizedException;

@ControllerAdvice
public class SecurityExceptionControllerAdvice {

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
}
