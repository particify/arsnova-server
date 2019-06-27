package de.thm.arsnova.web.exceptions;

/**
 * Payload Too Large means status code 413.
 */
public class PayloadTooLargeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PayloadTooLargeException() {
		super();
	}

	public PayloadTooLargeException(final String message) {
		super(message);
	}

	public PayloadTooLargeException(final Throwable e) {
		super(e);
	}

	public PayloadTooLargeException(final String message, final Throwable e) {
		super(message, e);
	}
}
