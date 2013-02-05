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
package de.thm.arsnova;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test to verify the public RESTlike API.
 * 
 * These tests only check the API communication and structure of responses. They do not verify data.
 */
public class HttpRestApiTest {
	public final String host = "localhost";
	public final int port = 8080;
	public final String pathPrefix = "/";
	
	private HttpURLConnection sendRequest(String path, String method, String accept, HashMap<String, String> parameters, String contentType, String body) throws IOException {
		HttpURLConnection conn;
		
		try {
			conn = (HttpURLConnection) (new URL("http", host, port, pathPrefix + path)).openConnection();
			System.out.println(conn.getURL().toExternalForm());
			
			conn.setRequestMethod(method);
			conn.setRequestProperty("Accept", accept);
			conn.setRequestProperty("Host", host + ":" + Integer.valueOf(port));
			
			if (null != body) {
				conn.setRequestProperty("Content-Type", contentType);
				conn.setRequestProperty("Content-Length", Integer.toString(body.getBytes().length));
				conn.setDoOutput(true);
				DataOutputStream out = new DataOutputStream(conn.getOutputStream());
				out.writeBytes(body);
				out.flush();
				out.close();
			}
		} catch (MalformedURLException e) {
			conn = null;
			e.printStackTrace();
		}
		
		return conn;
	}
	
	private HttpURLConnection sendRequest(String path, String method, String accept, HashMap<String, String> parameters) throws IOException {
		return sendRequest(path, method, accept, parameters, null, null);
	}
	
	private String transformInputToString(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		StringBuilder str = new StringBuilder();
		String line;
		while (null != (line = reader.readLine())) {
			str.append(line);
		}
		System.out.println(str);
		
		return str.toString();
	}
	
	private JSONObject transformInputToJsonObject(InputStream input) throws IOException, JSONException {
		return new JSONObject(transformInputToString(input));
	}
	
	private JSONArray transformInputToJsonArray(InputStream input) throws IOException, JSONException {
		return new JSONArray(transformInputToString(input));
	}

	@Ignore("Test not implemented")
	@Test
	public void testSession() throws Exception {

	}
	
	@Ignore("Test not implemented")
	@Test
	public void testQuestionByLecturer() throws Exception {

	}

	@Ignore("Test not implemented")
	@Test
	public void testQuestionByAudience() throws Exception {

	}

	@Ignore("Test not implemented")
	@Test
	public void testSocket() throws Exception {

	}

	@Test
	public void testCanteen() throws Exception {
		HttpURLConnection conn;
		JSONArray jsonArr;
		String responseBody;
		
		/* TODO: make test case more specific  */
		conn = sendRequest("canteen/menu/vote", "GET", "application/json", null);
		assertEquals(200, conn.getResponseCode());
		jsonArr = transformInputToJsonArray(conn.getInputStream());
		assertNotNull(jsonArr);
		
		conn = sendRequest("canteen/menu/vote/count", "GET", "text/plain", null);
		assertEquals(200, conn.getResponseCode());
		responseBody = transformInputToString(conn.getInputStream());
		Integer.valueOf(responseBody);
		
		/* TODO: implement test for POST /canteen/menu/vote */
	}

	@Test
	public void testStatistics() throws Exception {
		HttpURLConnection conn;
		JSONObject jsonObj;
		String responseBody;
		
		conn = sendRequest("statistics", "GET", "application/json", null);
		assertEquals(200, conn.getResponseCode());
		jsonObj = transformInputToJsonObject(conn.getInputStream());
		assertTrue(jsonObj.has("answers"));
		assertTrue(jsonObj.has("questions"));
		assertTrue(jsonObj.has("openSessions"));
		assertTrue(jsonObj.has("closedSessions"));
		assertTrue(jsonObj.has("activeUsers"));
		
		conn = sendRequest("statistics/activeusercount", "GET", "text/plain", null);
		assertEquals(200, conn.getResponseCode());
		responseBody = transformInputToString(conn.getInputStream());
		Integer.parseInt(responseBody);
		
		conn = sendRequest("statistics/sessioncount", "GET", "text/plain", null);
		assertEquals(200, conn.getResponseCode());
		responseBody = transformInputToString(conn.getInputStream());
		Integer.parseInt(responseBody);
	}
}