package de.thm.arsnova.web.exceptions;

/**
 * Forbidden means status code 403.
 */
public class ForbiddenException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ForbiddenException() {
		super();
	}

	public ForbiddenException(final String message) {
		super(message);
	}

	public ForbiddenException(final Throwable e) {
		super(e);
	}

	public ForbiddenException(final String message, final Throwable e) {
		super(message, e);
	}
}
