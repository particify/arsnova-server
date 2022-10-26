/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

/**
 * An {@link AutoConfigurationImportFilter} that only selects AutoConfiguration
 * classes needed for Spring Actuator.
 *
 * @author Daniel Gerhardt
 */
public class ActuatorAutoConfigurationFilter implements AutoConfigurationImportFilter {
	private static final List<String> actuatorClassesPrefixes = Arrays.asList(new String[] {
			"org.springframework.boot.actuate.autoconfigure.",
			"org.springframework.boot.autoconfigure.web.servlet."});

	@Override
	public boolean[] match(final String[] autoConfigurationClasses,
			final AutoConfigurationMetadata autoConfigurationMetadata) {
		final boolean[] results = new boolean[autoConfigurationClasses.length];
		for (int i = 0; i < results.length; i++) {
			results[i] = autoConfigurationClasses[i] != null
					&& (!autoConfigurationClasses[i].startsWith("org.springframework.boot")
					|| checkPrefix(autoConfigurationClasses[i]));
		}

		return results;
	}

	private boolean checkPrefix(final String className) {
		return actuatorClassesPrefixes.stream().anyMatch(p -> className.startsWith(p));
	}
}
