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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Test;

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

	private void assertEncodedEquals(final String key, final String expected, final String actual) {
		try {
			assertEquals(key + "=" + URLEncoder.encode(expected, "UTF-8"), actual);
		} catch (final UnsupportedEncodingException e) {
			fail(e.getLocalizedMessage());
		}
	}
}
