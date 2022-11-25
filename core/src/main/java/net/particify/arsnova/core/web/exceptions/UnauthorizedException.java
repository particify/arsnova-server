package net.particify.arsnova.core.web.exceptions;

import java.io.Serial;

/**
 * Unauthorized means status code 401.
 */
public class UnauthorizedException extends RuntimeException {
  @Serial
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
