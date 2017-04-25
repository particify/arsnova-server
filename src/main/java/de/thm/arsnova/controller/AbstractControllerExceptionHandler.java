package de.thm.arsnova.controller;

import java.util.HashMap;
import java.util.Map;

public class AbstractControllerExceptionHandler {
	protected Map<String, Object> handleException(Throwable e) {
		final Map<String, Object> result = new HashMap<>();
		result.put("errorType", e.getClass().getSimpleName());
		result.put("errorMessage", e.getMessage());

		return result;
	}
}
