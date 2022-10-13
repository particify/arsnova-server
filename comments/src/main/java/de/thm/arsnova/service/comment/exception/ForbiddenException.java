package de.thm.arsnova.service.comment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ForbiddenException extends ResponseStatusException {

  public ForbiddenException() {
    super(HttpStatus.FORBIDDEN);
  }

  public ForbiddenException(final String message) {
    super(HttpStatus.FORBIDDEN, message);
  }

  public ForbiddenException(final String message, final Throwable e) {
    super(HttpStatus.FORBIDDEN, message, e);
  }
}
