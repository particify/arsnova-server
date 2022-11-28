package net.particify.arsnova.core.web.exceptions;

import java.io.Serial;

/**
 * Bad Request means status code 400.
 */
public class BadRequestException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public BadRequestException() {
    super();
  }

  public BadRequestException(final String message) {
    super(message);
  }

  public BadRequestException(final Throwable e) {
    super(e);
  }

  public BadRequestException(final String message, final Throwable e) {
    super(message, e);
  }
}
