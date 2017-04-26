package de.thm.arsnova.exceptions;

/**
 * Forbidden means status code 403.
 */
public class ForbiddenException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ForbiddenException() {
		super();
	}

	public ForbiddenException(String message) {
		super(message);
	}

	public ForbiddenException(Throwable e) {
		super(e);
	}

	public ForbiddenException(String message, Throwable e) {
		super(message, e);
	}
}
