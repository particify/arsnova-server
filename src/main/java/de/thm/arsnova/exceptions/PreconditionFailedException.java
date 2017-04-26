package de.thm.arsnova.exceptions;

/**
 * Precondition Failed means status code 412.
 */
public class PreconditionFailedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PreconditionFailedException() {
		super();
	}

	public PreconditionFailedException(String message) {
		super(message);
	}

	public PreconditionFailedException(Throwable e) {
		super(e);
	}

	public PreconditionFailedException(String message, Throwable e) {
		super(message, e);
	}
}
