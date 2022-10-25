package net.particify.arsnova.core.web.exceptions;

/**
 * Not Found means status code 404.
 */
public class NotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NotFoundException() {
    super();
  }

  public NotFoundException(final String message) {
    super(message);
  }

  public NotFoundException(final Throwable e) {
    super(e);
  }

  public NotFoundException(final String message, final Throwable e) {
    super(message, e);
  }
}
