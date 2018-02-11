package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class LogEntry extends Entity {
	public enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL
	}

	private String id;
	private String rev;
	private Date creationTimestamp;
	private Date updateTimestamp;
	private String event;
	private int level;
	private Map<String, Object> payload;

	public LogEntry(@JsonProperty String event, @JsonProperty int level, @JsonProperty Map<String, Object> payload) {
		this.event = event;
		this.level = level;
		this.payload = payload;
	}

	@JsonView(View.Persistence.class)
	public String getId() {
		return id;
	}

	@JsonView(View.Persistence.class)
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView(View.Persistence.class)
	public String getRevision() {
		return rev;
	}

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

	@JsonView(View.Persistence.class)
	public String getEvent() {
		return event;
	}

	@JsonView(View.Persistence.class)
	public void setEvent(final String event) {
		this.event = event;
	}

	@JsonView(View.Persistence.class)
	public int getLevel() {
		return level;
	}

	@JsonView(View.Persistence.class)
	public void setLevel(final int level) {
		this.level = level;
	}

	@JsonView(View.Persistence.class)
	public void setLevel(final de.thm.arsnova.entities.migration.v2.LogEntry.LogLevel level) {
		this.level = level.ordinal();
	}

	@JsonView(View.Persistence.class)
	public Map<String, Object> getPayload() {
		return payload;
	}

	@JsonView(View.Persistence.class)
	public void setPayload(final Map<String, Object> payload) {
		this.payload = payload;
	}

	/**
	 * {@inheritDoc}
	 *
	 * The following fields of <tt>LogEntry</tt> are excluded from equality checks:
	 * {@link #payload}.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final LogEntry logEntry = (LogEntry) o;

		return level == logEntry.level &&
				Objects.equals(id, logEntry.id) &&
				Objects.equals(rev, logEntry.rev) &&
				Objects.equals(creationTimestamp, logEntry.creationTimestamp) &&
				Objects.equals(updateTimestamp, logEntry.updateTimestamp) &&
				Objects.equals(event, logEntry.event);
	}
}
