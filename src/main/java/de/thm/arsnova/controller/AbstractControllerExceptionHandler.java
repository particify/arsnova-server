package de.thm.arsnova.controller;

import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

public class AbstractControllerExceptionHandler {
	/* Since exception messages might contain sensitive data, they are not exposed by default. */
	@Value("${api.expose-error-messages:false}") private boolean exposeMessages;

	protected Map<String, Object> handleException(Throwable e) {
		final Map<String, Object> result = new HashMap<>();
		result.put("errorType", e.getClass().getSimpleName());
		if (exposeMessages) {
			result.put("errorMessage", e.getMessage());
		}

		return result;
	}
}
