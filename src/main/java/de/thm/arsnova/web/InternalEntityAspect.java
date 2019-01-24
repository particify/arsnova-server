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

import de.thm.arsnova.model.Entity;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This aspect ensures that entities marked for internal use are not serialized for the public API.
 *
 * @author Daniel Gerhardt
 */
@Aspect
public class InternalEntityAspect {
	private static final Logger logger = LoggerFactory.getLogger(InternalEntityAspect.class);

	@AfterReturning(pointcut = "execution(de.thm.arsnova.model.Entity+ de.thm.arsnova.controller.*.*(..))", returning = "entity")
	public void prohibitInternalEntitySerialization(final Entity entity) {
		logger.debug("Executing InternalEntityAspect for entity: {}", entity);

		if (entity.isInternal()) {
			throw new SecurityException("Serialization of internal entities is not allowed.");
		}
	}
}
