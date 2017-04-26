package de.thm.arsnova.controller;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

public class AbstractControllerExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(AbstractControllerExceptionHandler.class);

	/* Since exception messages might contain sensitive data, they are not exposed by default. */
	@Value("${api.expose-error-messages:false}") private boolean exposeMessages;

	protected Map<String, Object> handleException(@NonNull Throwable e, @NonNull Level level) {
		final String message = e.getMessage() != null ? e.getMessage() : "";
		log(level, message, e);
		final Map<String, Object> result = new HashMap<>();
		result.put("errorType", e.getClass().getSimpleName());
		if (exposeMessages) {
			result.put("errorMessage", e.getMessage());
		}

		return result;
	}

	private void log(Level level, String message, Throwable e) {
		switch (level) {
			case ERROR:
				logger.error(message, e);
				break;
			case WARN:
				logger.warn(message, e);
				break;
			case INFO:
				logger.info(message, e);
				break;
			case DEBUG:
				logger.debug(message, e);
				break;
			case TRACE:
				logger.trace(message, e);
				break;
		}
	}
}
