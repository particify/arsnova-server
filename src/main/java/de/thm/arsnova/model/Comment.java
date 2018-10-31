package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class Comment extends Entity {
	private String roomId;
	private String creatorId;
	private String subject;
	private String body;
	private Date timestamp;
	private boolean read;
	private Map<String, Map<String, ?>> extensions;

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRoomId() {
		return roomId;
	}

	@JsonView(View.Persistence.class)
	public void setRoomId(final String roomId) {
		this.roomId = roomId;
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
	public Date getTimestamp() {
		return (Date)timestamp.clone();
	}

	@JsonView(View.Persistence.class)
	public void setTimestamp(Date timestamp) {
		this.timestamp = (Date)timestamp.clone();
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isRead() {
		return read;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRead(final boolean read) {
		this.read = read;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Map<String, Map<String, ?>> getExtensions() {
		return extensions;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setExtensions(Map<String, Map<String, ?>> extensions) {
		this.extensions = extensions;
	}

	/**
	 * {@inheritDoc}
	 *
	 * The following fields of <tt>LogEntry</tt> are excluded from equality checks:
	 * {@link #extensions}.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final Comment comment = (Comment) o;

		return read == comment.read &&
				Objects.equals(roomId, comment.roomId) &&
				Objects.equals(creatorId, comment.creatorId) &&
				Objects.equals(subject, comment.subject) &&
				Objects.equals(body, comment.body) &&
				Objects.equals(timestamp, comment.timestamp) &&
				Objects.equals(extensions, comment.extensions);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("roomId", roomId)
				.append("creatorId", creatorId)
				.append("subject", subject)
				.append("body", body)
				.append("timestamp", timestamp)
				.append("read", read);
	}
}
