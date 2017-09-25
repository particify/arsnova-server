package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Date;
import java.util.List;

public class MigrationState implements Entity {
	public class Migration {
		private String id;
		private Date start;

		public Migration(String id, Date start) {
			this.id = id;
			this.start = start;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public String getId() {
			return id;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public Date getStart() {
			return start;
		}
	}

	private String id = "MigrationState";
	private String rev;
	private Date creationTimestamp;
	private Date updateTimestamp;
	private Migration active;
	private List<String> completed;

	@Override
	@JsonView(View.Persistence.class)
	public String getId() {
		return id;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setId(final String id) {
		if (!id.equals(this.id)) {
			throw new IllegalArgumentException("ID of this entity must not be changed.");
		};
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@Override
	@JsonView(View.Persistence.class)
	public Date getCreationTimestamp() {
		return creationTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setCreationTimestamp(final Date creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setUpdateTimestamp(final Date updateTimestamp) {
		this.updateTimestamp = updateTimestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Migration getActive() {
		return active;
	}

	@JsonView(View.Persistence.class)
	public void setActive(final Migration active) {
		this.active = active;
	}

	public void setActive(final String id, final Date start) {
		this.setActive(new Migration(id, start));
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public List<String> getCompleted() {
		return completed;
	}

	@JsonView(View.Persistence.class)
	public void setCompleted(final List<String> completed) {
		this.completed = completed;
	}
}
