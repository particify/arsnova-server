package de.thm.arsnova.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.serialization.View;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.Map;

public class EntityService<T extends Entity> {
	protected Class<T> type;
	protected CrudRepository<T, String> repository;
	private ObjectMapper objectMapper;

	public EntityService(Class<T> type, CrudRepository<T, String> repository, ObjectMapper objectMapper) {
		this.type = type;
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@PreAuthorize("hasPermission(type, #id, 'read')")
	public T get(final String id) {
		return repository.findOne(id);
	}

	@PreAuthorize("hasPermission(#entity, 'create')")
	public T create(final T entity) {
		if (entity.getId() != null || entity.getRevision() != null) {
			throw new IllegalArgumentException("Entity is not new.");
		}
		return repository.save(entity);
	}

	@PreAuthorize("hasPermission(#oldEntity, 'update')")
	public T update(final T oldEntity, final T newEntity) {
		newEntity.setId(oldEntity.getId());
		return repository.save(newEntity);
	}

	@PreAuthorize("hasPermission(#entity, 'update')")
	public T patch(final T entity, final Map<String, Object> changes) throws IOException {
		ObjectReader reader = objectMapper.readerForUpdating(entity).withView(View.Public.class);
		JsonNode tree = objectMapper.valueToTree(changes);
		reader.readValue(tree);

		return repository.save(entity);
	}

	@PreAuthorize("hasPermission(#entity, 'delete')")
	public void delete(final T entity) {
		repository.delete(entity);
	}
}
