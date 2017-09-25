package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.Date;

public class Motd implements Entity {
	private String id;
	private String rev;
	private Date creationTimestamp;
	private Date updateTimestamp;
	private String sessionId;
	private Date startdate;
	private Date enddate;
	private String title;
	private String body;
	private String audience;

	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
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
	public String getSessionId() {
		return sessionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getStartdate() {
		return startdate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setStartdate(final Date timestamp) {
		startdate = timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public Date getEnddate() {
		return enddate;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setEnddate(final Date timestamp) {
		enddate = timestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getTitle() {
		return title;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTitle(final String ttitle) {
		title = ttitle;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getBody() {
		return body;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setBody(final String ttext) {
		body = ttext;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getAudience() {
		return audience;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAudience(final String a) {
		audience = a;
	}
}
