package de.thm.arsnova.exceptions;

/**
 * Bad Request means status code 400.
 */
public class BadRequestException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public BadRequestException() {
		super();
	}

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(Throwable e) {
		super(e);
	}

	public BadRequestException(String message, Throwable e) {
		super(message, e);
	}
}
