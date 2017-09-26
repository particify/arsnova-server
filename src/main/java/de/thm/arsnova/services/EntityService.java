package de.thm.arsnova.services;

import de.thm.arsnova.entities.Entity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface EntityService<T extends Entity> {
	@PreAuthorize("hasPermission(#id, #this.this.getTypeName(), 'read')")
	T get(String id);

	@PreAuthorize("hasPermission(#entity, 'create')")
	T create(T entity);

	@PreAuthorize("hasPermission(#oldEntity, 'update')")
	T update(T oldEntity, T newEntity);

	@PreAuthorize("hasPermission(#entity, 'update')")
	T patch(T entity, Map<String, Object> changes) throws IOException;

	@PreFilter(value = "hasPermission(filterObject, 'update')", filterTarget = "entities")
	Iterable<T> patch(Collection<T> entities, Map<String, Object> changes) throws IOException;

	@PreAuthorize("hasPermission(#entity, 'delete')")
	void delete(T entity);
}
