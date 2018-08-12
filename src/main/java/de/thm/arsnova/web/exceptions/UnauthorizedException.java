package de.thm.arsnova.web.exceptions;

/**
 * Unauthorized means status code 401.
 */
public class UnauthorizedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UnauthorizedException() {
		super();
	}

	public UnauthorizedException(String message) {
		super(message);
	}

	public UnauthorizedException(Throwable e) {
		super(e);
	}

	public UnauthorizedException(String message, Throwable e) {
		super(message, e);
	}
}
