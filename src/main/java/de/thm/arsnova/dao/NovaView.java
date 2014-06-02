/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
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

import com.fourspaces.couchdb.View;

public class NovaView extends View {

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

	private String toJsonArray(final String[] strings) {
		final StringBuilder sb = new StringBuilder();
		for (final String str : strings) {
			if (isNumber(str)) {
				sb.append(str + ",");
			} else if (str.equals("{}")) {
				sb.append(str + ",");
			} else {
				sb.append("\"" + str + "\"" + ",");
			}
		}
		// remove final comma
		sb.replace(sb.length() - 1, sb.length(), "");
		sb.insert(0, "[");
		sb.append("]");
		return encode(sb.toString());
	}

	private String quote(final String string) {
		return encode("\"" + string + "\"");
	}

	private boolean isNumber(final String string) {
		return string.matches("^[0-9]+$");
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
