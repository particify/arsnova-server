/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import com.fourspaces.couchdb.View.StaleMode;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import static org.junit.Assert.*;

public class NovaViewTest {

	@Test
	public void setKeyShouldAcceptSingleArgument() {
		final NovaView v = new NovaView(null);
		v.setKey("foo");
		assertEncodedEquals("key", "\"foo\"", v.getQueryString());
	}

	@Test
	public void setKeyShouldAcceptMultipleArgument() {
		final NovaView v = new NovaView(null);
		v.setKey("foo", "bar", "baz");
		assertEncodedEquals("key", "[\"foo\",\"bar\",\"baz\"]", v.getQueryString());
	}

	@Test
	public void setStartKeyShouldAcceptSingleArgument() {
		final NovaView v = new NovaView(null);
		v.setStartKey("foo");
		assertEncodedEquals("startkey", "\"foo\"", v.getQueryString());
	}

	@Test
	public void setStartKeyShouldAcceptSingleArgumentArray() {
		final NovaView v = new NovaView(null);
		v.setStartKeyArray("foo");
		assertEncodedEquals("startkey", "[\"foo\"]", v.getQueryString());
	}

	@Test
	public void setEndKeyShouldAcceptSingleArgumentArray() {
		final NovaView v = new NovaView(null);
		v.setEndKeyArray("foo");
		assertEncodedEquals("endkey", "[\"foo\"]", v.getQueryString());
	}

	@Test
	public void setEndKeyShouldAcceptSingleArgument() {
		final NovaView v = new NovaView(null);
		v.setEndKey("foo");
		assertEncodedEquals("endkey", "\"foo\"", v.getQueryString());
	}

	@Test
	public void setStartKeyShouldAcceptMultipleArgument() {
		final NovaView v = new NovaView(null);
		v.setStartKey("foo", "bar", "baz");
		assertEncodedEquals("startkey", "[\"foo\",\"bar\",\"baz\"]", v.getQueryString());
	}

	@Test
	public void setEndKeyShouldAcceptMultipleArgument() {
		final NovaView v = new NovaView(null);
		v.setEndKey("foo", "bar", "baz");
		assertEncodedEquals("endkey", "[\"foo\",\"bar\",\"baz\"]", v.getQueryString());
	}

	@Test
	public void keysShouldSupportEmptyObject() {
		final NovaView v = new NovaView(null);
		v.setKey("foo", "bar", "{}");
		assertEncodedEquals("key", "[\"foo\",\"bar\",{}]", v.getQueryString());
	}

	@Test
	public void arrayKeysShouldNotEnquoteNumbers() {
		final NovaView v = new NovaView(null);
		v.setKey("foo", "bar", "2");
		assertEncodedEquals("key", "[\"foo\",\"bar\",2]", v.getQueryString());
	}

	@Test
	public void singleArrayKeysShouldNotEnquoteNumbers() {
		final NovaView v1 = new NovaView(null);
		final NovaView v2 = new NovaView(null);
		v1.setStartKeyArray("2");
		v2.setEndKeyArray("2");
		assertEncodedEquals("startkey", "[2]", v1.getQueryString());
		assertEncodedEquals("endkey", "[2]", v2.getQueryString());
	}

	@Test
	public void shouldSupportAddingKeysParameter() {
		String[] stringKeys = new String[] { "foo", "bar" };
		String[] numberKeys = new String[] { "123", "456" };
		String[] mixedKeys = new String[] { "foo", "123" };
		String[] arrayKeys = new String[] { "[\"foo\",123]", "[456,\"bar\"]" };
		String[] emptyKeys = new String[0];
		final NovaView v1 = new NovaView(null);
		final NovaView v2 = new NovaView(null);
		final NovaView v3 = new NovaView(null);
		final NovaView v4 = new NovaView(null);
		final NovaView v5 = new NovaView(null);
		v1.setKeys(Arrays.asList(stringKeys));
		v2.setKeys(Arrays.asList(numberKeys));
		v3.setKeys(Arrays.asList(mixedKeys));
		v4.setKeys(Arrays.asList(arrayKeys));
		v5.setKeys(Arrays.asList(emptyKeys));
		assertEncodedEquals("keys", "[\"foo\",\"bar\"]", v1.getQueryString());
		assertEncodedEquals("keys", "[123,456]", v2.getQueryString());
		assertEncodedEquals("keys", "[\"foo\",123]", v3.getQueryString());
		assertEncodedEquals("keys", "[[\"foo\",123],[456,\"bar\"]]", v4.getQueryString());
		assertEncodedEquals("keys", "[]", v5.getQueryString());
	}

	@Test
	public void shouldSupportStaleViews() {
		final NovaView v1 = new NovaView(null);
		final NovaView v2 = new NovaView(null);
		final NovaView v3 = new NovaView(null);
		final NovaView v4 = new NovaView(null);
		v1.setStale(StaleMode.NONE);
		v2.setStale(StaleMode.OK);
		v3.setStale(StaleMode.UPDATE_AFTER);
		assertNull(v1.getQueryString());
		assertEncodedEquals("stale", "ok", v2.getQueryString());
		assertEncodedEquals("stale", "update_after", v3.getQueryString());
		assertNull(v4.getQueryString());
	}

	@Test
	public void shouldSupportIncludeDocsParameter() {
		final NovaView v1 = new NovaView(null);
		final NovaView v2 = new NovaView(null);
		final NovaView v3 = new NovaView(null);
		v1.setIncludeDocs(true);
		v2.setIncludeDocs(false);
		assertEncodedEquals("include_docs", "true", v1.getQueryString());
		assertNull(v2.getQueryString());
		assertNull(v3.getQueryString());
	}

	private void assertEncodedEquals(final String key, final String expected, final String actual) {
		try {
			assertEquals(key + "=" + URLEncoder.encode(expected, "UTF-8"), actual);
		} catch (final UnsupportedEncodingException e) {
			fail(e.getLocalizedMessage());
		}
	}
}
