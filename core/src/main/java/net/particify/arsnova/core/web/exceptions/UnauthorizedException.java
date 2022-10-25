package net.particify.arsnova.core.web.exceptions;

/**
 * Unauthorized means status code 401.
 */
public class UnauthorizedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UnauthorizedException() {
    super();
  }

  public UnauthorizedException(final String message) {
    super(message);
  }

  public UnauthorizedException(final Throwable e) {
    super(e);
  }

  public UnauthorizedException(final String message, final Throwable e) {
    super(message, e);
  }
}
