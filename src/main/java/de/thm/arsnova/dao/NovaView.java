/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.dao;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.fourspaces.couchdb.View;

public class NovaView extends View {

	public enum StaleMode {
		NONE, OK, UPDATE_AFTER
	}

	protected String keys;

	protected StaleMode stale = StaleMode.NONE;

	public NovaView(final String fullname) {
		super(fullname);
	}

	@Override
	public void setStartKey(final String key) {
		startKey = quote(key);
	}

	public void setStartKeyArray(final String key) {
		if (isNumber(key)) {
			startKey = encode("[" + key + "]");
		} else {
			startKey = encode("[\"" + key + "\"]");
		}
	}

	public void setStartKeyArray(final String... keys) {
		this.setStartKey(keys);
	}

	@Override
	public void setEndKey(final String key) {
		endKey = quote(key);
	}

	public void setEndKeyArray(final String key) {
		if (isNumber(key)) {
			endKey = encode("[" + key + "]");
		} else {
			endKey = encode("[\"" + key + "\"]");
		}
	}

	public void setEndKeyArray(final String... keys) {
		this.setEndKey(keys);
	}

	public void setStartKey(final String... keys) {
		startKey = toJsonArray(keys);
	}

	public void setEndKey(final String... keys) {
		endKey = toJsonArray(keys);
	}

	@Override
	public void setKey(final String key) {
		this.key = quote(key);
	}

	public void setKey(final String... keys) {
		key = toJsonArray(keys);
	}

	public void setKeys(List<String> keys) {
		this.keys = toJsonArray(keys.toArray(new String[keys.size()]));
	}

	public void setStale(StaleMode stale) {
		this.stale = stale;
	}

	@Override
	public String getQueryString() {
		final String tempQuery = super.getQueryString();
		final StringBuilder query = new StringBuilder();
		if (tempQuery != null) {
			query.append(tempQuery);
		}
		if (keys != null) {
			if (query.length() > 0) {
				query.append("&");
			}
			query.append("keys=" + keys);
		}
		if (stale != null && stale != StaleMode.NONE) {
			if (query.length() > 0) {
				query.append("&");
			}
			if (stale == StaleMode.OK) {
				query.append("stale=ok");
			} else if (stale == StaleMode.UPDATE_AFTER) {
				query.append("stale=update_after");
			}
		}

		if (query.length() == 0) {
			return null;
		}
		return query.toString();
	}

	private String toJsonArray(final String[] strs) {
		final List<String> strings = new ArrayList<String>();
		for (final String string : strs) {
			if (isNumber(string) || isPlaceholder(string) || isArray(string)) {
				strings.add(string);
			} else {
				strings.add("\"" + string + "\"");
			}
		}
		return encode("[" + StringUtils.join(strings, ",") + "]");
	}

	private String quote(final String string) {
		return encode("\"" + string + "\"");
	}

	private boolean isNumber(final String string) {
		return string.matches("^[0-9]+$");
	}

	private boolean isPlaceholder(final String string) {
		return string.equals("{}");
	}

	private boolean isArray(final String string) {
		return string.startsWith("[") && string.endsWith("]");
	}

	private String encode(final String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			// Since we're using 'UTF-8', this should Exception should never occur.
			return "";
		}
	}
}
