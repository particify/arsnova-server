package net.particify.arsnova.core.web.exceptions;

import java.io.Serial;

/**
 * Forbidden means status code 403.
 */
public class ForbiddenException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public ForbiddenException() {
    super();
  }

  public ForbiddenException(final String message) {
    super(message);
  }

  public ForbiddenException(final Throwable e) {
    super(e);
  }

  public ForbiddenException(final String message, final Throwable e) {
    super(message, e);
  }
}
