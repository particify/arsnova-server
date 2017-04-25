package de.thm.arsnova.controller;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class DefaultExceptionHandler {
	@ExceptionHandler
	@ResponseBody
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Map<String, String> defaultExceptionHandler(
			final Exception e,
			final HttpServletRequest req
	) throws Exception {
		/* If the exception is annotated with @ResponseStatus rethrow it and let
		 * the framework handle it.
		 * See https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc. */
		if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
			throw e;
		}
		final Map<String, String> result = new HashMap<>();
		result.put("code", "500");
		result.put("status", "Internal server error");
		result.put("message", e.getMessage());

		return result;
	}
}
