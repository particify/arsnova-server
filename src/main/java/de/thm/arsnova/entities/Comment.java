package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

public class Comment implements Entity {
	private String id;
	private String rev;
	private String sessionId;
	private String creatorId;
	private String subject;
	private String body;
	private long timestamp;
	private boolean read;

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getSessionId() {
		return sessionId;
	}

	@JsonView(View.Persistence.class)
	public void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	@JsonView(View.Persistence.class)
	public String getCreatorId() {
		return creatorId;
	}

	@JsonView(View.Persistence.class)
	public void setCreatorId(final String creatorId) {
		this.creatorId = creatorId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getSubject() {
		return subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSubject(final String subject) {
		this.subject = subject;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getBody() {
		return body;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setBody(final String body) {
		this.body = body;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public long getTimestamp() {
		return timestamp;
	}

	@JsonView(View.Persistence.class)
	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isRead() {
		return read;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRead(final boolean read) {
		this.read = read;
	}
}
