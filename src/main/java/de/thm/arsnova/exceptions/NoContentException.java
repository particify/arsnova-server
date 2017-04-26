package de.thm.arsnova.exceptions;

/**
 * No Content means status code 204.
 */
public class NoContentException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoContentException() {
		super();
	}

	public NoContentException(String message) {
		super(message);
	}

	public NoContentException(Throwable e) {
		super(e);
	}

	public NoContentException(String message, Throwable e) {
		super(message, e);
	}
}
