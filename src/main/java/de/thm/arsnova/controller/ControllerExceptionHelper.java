/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.controller;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ControllerExceptionHelper {
	private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHelper.class);

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
