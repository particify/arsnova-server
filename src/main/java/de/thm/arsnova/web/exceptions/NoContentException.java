package de.thm.arsnova.web.exceptions;

/**
 * No Content means status code 204.
 */
public class NoContentException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoContentException() {
		super();
	}

	public NoContentException(final String message) {
		super(message);
	}

	public NoContentException(final Throwable e) {
		super(e);
	}

	public NoContentException(final String message, final Throwable e) {
		super(message, e);
	}
}
