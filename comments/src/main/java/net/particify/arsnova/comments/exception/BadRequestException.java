package net.particify.arsnova.comments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BadRequestException extends ResponseStatusException {

  public BadRequestException() {
    super(HttpStatus.BAD_REQUEST);
  }

  public BadRequestException(final String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  public BadRequestException(final String message, final Throwable e) {
    super(HttpStatus.BAD_REQUEST, message, e);
  }
}
