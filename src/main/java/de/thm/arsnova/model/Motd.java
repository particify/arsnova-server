package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

import java.util.Date;
import java.util.Objects;

public class Motd extends Entity {
	public enum Audience {
		ALL,
		AUTHENTICATED,
		AUTHORS,
		PARTICIPANTS,
		ROOM
	}

	private String roomId;
	private Date startDate;
	private Date endDate;
	private String title;
	private String body;
	private Audience audience;

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
	public String getRoomId() {
		return roomId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRoomId(final String roomId) {
		this.roomId = roomId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getStartDate() {
		return startDate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setStartDate(final Date startDate) {
		this.startDate = startDate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getEndDate() {
		return endDate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setEndDate(final Date endDate) {
		this.endDate = endDate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getTitle() {
		return title;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTitle(final String title) {
		this.title = title;
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
	public Audience getAudience() {
		return audience;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAudience(final Audience audience) {
		this.audience = audience;
	}

	/**
	 * {@inheritDoc}
	 *
	 * All fields of <tt>Motd</tt> are included in equality checks.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final Motd motd = (Motd) o;

		return Objects.equals(roomId, motd.roomId) &&
				Objects.equals(startDate, motd.startDate) &&
				Objects.equals(endDate, motd.endDate) &&
				Objects.equals(title, motd.title) &&
				Objects.equals(body, motd.body) &&
				audience == motd.audience;
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("roomId", roomId)
				.append("startDate", startDate)
				.append("endDate", endDate)
				.append("title", title)
				.append("body", body)
				.append("audience", audience);
	}
}
