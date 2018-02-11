package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.MINIMAL_CLASS,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type"
)
public abstract class Answer extends Entity {
	private String contentId;
	private String roomId;
	private String creatorId;
	private int round;
	private Map<String, Map<String, ?>> extensions;

	@JsonView({View.Persistence.class, View.Public.class})
	public String getContentId() {
		return contentId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setContentId(final String contentId) {
		this.contentId = contentId;
	}

	@JsonView(View.Persistence.class)
	public String getRoomId() {
		return roomId;
	}

	@JsonView(View.Persistence.class)
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	@JsonView(View.Persistence.class)
	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(final String creatorId) {
		this.creatorId = creatorId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getRound() {
		return round;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRound(final int round) {
		this.round = round;
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
	 * The following fields of <tt>Answer</tt> are excluded from equality checks:
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
		final Answer answer = (Answer) o;

		return round == answer.round &&
				Objects.equals(contentId, answer.contentId) &&
				Objects.equals(roomId, answer.roomId) &&
				Objects.equals(creatorId, answer.creatorId);
	}
}
