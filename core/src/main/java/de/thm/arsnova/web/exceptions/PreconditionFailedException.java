package de.thm.arsnova.web.exceptions;

/**
 * Precondition Failed means status code 412.
 */
public class PreconditionFailedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PreconditionFailedException() {
    super();
  }

  public PreconditionFailedException(final String message) {
    super(message);
  }

  public PreconditionFailedException(final Throwable e) {
    super(e);
  }

  public PreconditionFailedException(final String message, final Throwable e) {
    super(message, e);
  }
}
