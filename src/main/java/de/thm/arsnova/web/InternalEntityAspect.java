package de.thm.arsnova.web;

import de.thm.arsnova.entities.Entity;
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

	@AfterReturning(pointcut = "execution(de.thm.arsnova.entities.Entity+ de.thm.arsnova.controller.*.*(..))", returning = "entity")
	public void prohibitInternalEntitySerialization(final Entity entity) {
		logger.debug("Executing InternalEntityAspect for entity: {}", entity);

		if (entity.isInternal()) {
			throw new SecurityException("Serialization of internal entities is not allowed.");
		}
	}
}
