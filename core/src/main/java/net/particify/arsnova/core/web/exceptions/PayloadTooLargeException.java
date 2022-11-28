package net.particify.arsnova.core.web.exceptions;

import java.io.Serial;

/**
 * Payload Too Large means status code 413.
 */
public class PayloadTooLargeException extends RuntimeException {
  @Serial
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
