package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Map;

public abstract class Answer implements Entity {
	private String id;
	private String rev;
	private String contentId;
	private String roomId;
	private String creatorId;
	private int round;
	private Map<String, Map<String, ?>> extensions;

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
}
