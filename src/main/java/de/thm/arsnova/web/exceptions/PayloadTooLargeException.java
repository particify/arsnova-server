package de.thm.arsnova.web.exceptions;

/**
 * Payload Too Large means status code 413.
 */
public class PayloadTooLargeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PayloadTooLargeException() {
		super();
	}

	public PayloadTooLargeException(String message) {
		super(message);
	}

	public PayloadTooLargeException(Throwable e) {
		super(e);
	}

	public PayloadTooLargeException(String message, Throwable e) {
		super(message, e);
	}
}
