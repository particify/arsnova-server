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

	public NovaView(String fullname) {
		super(fullname);
	}

	@Override
	public void setStartKey(String key) {
		this.startKey = quote(key);
	}

	public void setStartKeyArray(String key) {
		if (isNumber(key)) {
			this.startKey = encode("[" + key + "]");
		} else {
			this.startKey = encode("[\"" + key + "\"]");
		}
	}

	public void setStartKeyArray(String... keys) {
		this.setStartKey(keys);
	}

	@Override
	public void setEndKey(String key) {
		this.endKey = quote(key);
	}

	public void setEndKeyArray(String key) {
		if (isNumber(key)) {
			this.endKey = encode("[" + key + "]");
		} else {
			this.endKey = encode("[\"" + key + "\"]");
		}
	}

	public void setEndKeyArray(String... keys) {
		this.setEndKey(keys);
	}

	public void setStartKey(String... keys) {
		this.startKey = toJsonArray(keys);
	}

	public void setEndKey(String... keys) {
		this.endKey = toJsonArray(keys);
	}

	@Override
	public void setKey(String key) {
		this.key = quote(key);
	}

	public void setKey(String... keys) {
		this.key = toJsonArray(keys);
	}

	private String toJsonArray(String[] strings) {
		StringBuilder sb = new StringBuilder();
		for (String str : strings) {
			if (isNumber(str)) {
				sb.append(str + ",");
			} else if (str.equals("{}")) {
				sb.append(str + ",");
			} else {
				sb.append("\"" + str + "\"" + ",");
			}
		}
		sb.replace(sb.length() - 1, sb.length(), ""); // remove final comma
		sb.insert(0, "[");
		sb.append("]");
		return encode(sb.toString());
	}

	private String quote(String string) {
		return encode("\"" + string + "\"");
	}
	
	private boolean isNumber(String string) {
		return string.matches("^[0-9]+$");
	}

	private String encode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Since we're using 'UTF-8', this should Exception should never occur.
		}
		return "";
	}
}
