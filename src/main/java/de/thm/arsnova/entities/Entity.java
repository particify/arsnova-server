package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Date;

public interface Entity {
	String getId();
	void setId(String id);

	String getRevision();
	void setRevision(String rev);

	Date getCreationTimestamp();
	void setCreationTimestamp(Date creationTimestamp);

	Date getUpdateTimestamp();
	void setUpdateTimestamp(Date updateTimestamp);

	@JsonView(View.Persistence.class)
	default Class<? extends Entity> getType() {
		return getClass();
	}
}
