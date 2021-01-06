/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class NovaViewTest {

	@Test
	public void setKeyShouldAcceptSingleArgument() {
		final NovaView v = new NovaView(null);
		v.setKey("foo");
		assertEncodedContains("key", "\"foo\"", v.getQueryString());
	}

	@Test
	public void setKeyShouldAcceptMultipleArgument() {
		final NovaView v = new NovaView(null);
		v.setKey("foo", "bar", "baz");
		assertEncodedContains("key", "[\"foo\",\"bar\",\"baz\"]", v.getQueryString());
	}

	@Test
	public void setStartKeyShouldAcceptSingleArgument() {
		final NovaView v = new NovaView(null);
		v.setStartKey("foo");
		assertEncodedContains("startkey", "\"foo\"", v.getQueryString());
	}

	@Test
	public void setStartKeyShouldAcceptSingleArgumentArray() {
		final NovaView v = new NovaView(null);
		v.setStartKeyArray("foo");
		assertEncodedContains("startkey", "[\"foo\"]", v.getQueryString());
	}

	@Test
	public void setEndKeyShouldAcceptSingleArgumentArray() {
		final NovaView v = new NovaView(null);
		v.setEndKeyArray("foo");
		assertEncodedContains("endkey", "[\"foo\"]", v.getQueryString());
	}

	@Test
	public void setEndKeyShouldAcceptSingleArgument() {
		final NovaView v = new NovaView(null);
		v.setEndKey("foo");
		assertEncodedContains("endkey", "\"foo\"", v.getQueryString());
	}

	@Test
	public void setStartKeyShouldAcceptMultipleArgument() {
		final NovaView v = new NovaView(null);
		v.setStartKey("foo", "bar", "baz");
		assertEncodedContains("startkey", "[\"foo\",\"bar\",\"baz\"]", v.getQueryString());
	}

	@Test
	public void setEndKeyShouldAcceptMultipleArgument() {
		final NovaView v = new NovaView(null);
		v.setEndKey("foo", "bar", "baz");
		assertEncodedContains("endkey", "[\"foo\",\"bar\",\"baz\"]", v.getQueryString());
	}

	@Test
	public void keysShouldSupportEmptyObject() {
		final NovaView v = new NovaView(null);
		v.setKey("foo", "bar", "{}");
		assertEncodedContains("key", "[\"foo\",\"bar\",{}]", v.getQueryString());
	}

	@Test
	public void arrayKeysShouldEnquoteNumbers() {
		final NovaView v = new NovaView(null);
		v.setKey("foo", "bar", "2");
		assertEncodedContains("key", "[\"foo\",\"bar\",\"2\"]", v.getQueryString());
	}

	@Test
	public void singleArrayKeysShouldEnquoteNumbers() {
		final NovaView v1 = new NovaView(null);
		final NovaView v2 = new NovaView(null);
		v1.setStartKeyArray("2");
		v2.setEndKeyArray("2");
		assertEncodedContains("startkey", "[\"2\"]", v1.getQueryString());
		assertEncodedContains("endkey", "[\"2\"]", v2.getQueryString());
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
		assertEncodedContains("keys", "[\"foo\",\"bar\"]", v1.getQueryString());
		assertEncodedContains("keys", "[\"123\",\"456\"]", v2.getQueryString());
		assertEncodedContains("keys", "[\"foo\",\"123\"]", v3.getQueryString());
		assertEncodedContains("keys", "[\"[\\\"foo\\\",123]\",\"[456,\\\"bar\\\"]\"]", v4.getQueryString());
		assertEncodedContains("keys", "[]", v5.getQueryString());
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
		assertThat(v1.getQueryString(), not(containsString("stale")));
		assertEncodedContains("stale", "ok", v2.getQueryString());
		assertEncodedContains("stale", "update_after", v3.getQueryString());
		assertThat(v4.getQueryString(), not(containsString("stale")));
	}

	@Test
	public void shouldSupportIncludeDocsParameter() {
		final NovaView v1 = new NovaView(null);
		final NovaView v2 = new NovaView(null);
		final NovaView v3 = new NovaView(null);
		v1.setIncludeDocs(true);
		v2.setIncludeDocs(false);
		assertEncodedContains("include_docs", "true", v1.getQueryString());
		assertEncodedContainsNot("include_docs", "true", v2.getQueryString());
		assertEncodedContainsNot("include_docs", "true", v3.getQueryString());
	}

	private void assertEncodedContains(final String key, final String expected, final String actual) {
		try {
			assertThat(actual, containsString(key + "=" + URLEncoder.encode(expected, "UTF-8")));
		} catch (final UnsupportedEncodingException e) {
			fail(e.getLocalizedMessage());
		}
	}

	private void assertEncodedContainsNot(final String key, final String expected, final String actual) {
		try {
			assertThat(actual, not(containsString(key + "=" + URLEncoder.encode(expected, "UTF-8"))));
		} catch (final UnsupportedEncodingException e) {
			fail(e.getLocalizedMessage());
		}
	}
}
