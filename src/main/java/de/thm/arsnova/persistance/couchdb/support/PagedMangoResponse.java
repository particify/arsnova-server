package de.thm.arsnova.persistance.couchdb.support;

import java.util.List;

/**
 * Contains the entities of the response and the bookmark to query the next page.
 *
 * @param <T>
 * @author Daniel Gerhardt
 */
public class PagedMangoResponse<T> {
	private List<T> entities;
	private String bookmark;

	public PagedMangoResponse(final List<T> entities, final String bookmark) {
		this.entities = entities;
		this.bookmark = bookmark;
	}

	public List<T> getEntities() {
		return entities;
	}

	public String getBookmark() {
		return bookmark;
	}
}
