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

package de.thm.arsnova.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows specifying HTTP cache headers.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheControl {
	enum Policy {
		NO_CACHE("no-cache"),
		NO_STORE("no-store"),
		PRIVATE("private"),
		PUBLIC("public");

		Policy() {
			this.policyString = null;
		}

		Policy(final String policyString) {
			this.policyString = policyString;
		}

		public String getPolicyString() {
			return this.policyString;
		}

		private final String policyString;
	}

	boolean noCache() default false;

	int maxAge() default 0;
	Policy[] policy() default {};
}
