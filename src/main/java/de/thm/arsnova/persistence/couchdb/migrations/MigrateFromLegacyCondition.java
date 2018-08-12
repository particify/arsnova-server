package de.thm.arsnova.persistence.couchdb.migrations;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

@Component
public class MigrateFromLegacyCondition implements Condition {
	@Override
	public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
		final String migrateFrom = context.getEnvironment().getProperty("couchdb.migrate-from");
		return migrateFrom != null && !migrateFrom.isEmpty();
	}
}
