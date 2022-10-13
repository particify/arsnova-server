package de.thm.arsnova.web.exceptions;

/**
 * Not Implemented means status code 501.
 */
public class NotImplementedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NotImplementedException() {
    super();
  }

  public NotImplementedException(final String message) {
    super(message);
  }

  public NotImplementedException(final Throwable e) {
    super(e);
  }

  public NotImplementedException(final String message, final Throwable e) {
    super(message, e);
  }
}
