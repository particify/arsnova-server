/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.controller;

/**
 * Base class of all controllers.
 */
public class AbstractController {
	protected static final String X_DEPRECATED_API = "X-Deprecated-API";
	protected static final String X_FORWARDED = "X-Forwarded";
	protected static final String HTML_STATUS_200 = "OK";
	protected static final String HTML_STATUS_201 = "Created";
	protected static final String HTML_STATUS_204 = "No Content";
	protected static final String HTML_STATUS_400 = "Bad request";
	protected static final String HTML_STATUS_403 = "Forbidden";
	protected static final String HTML_STATUS_404 = "Not Found";
	protected static final String HTML_STATUS_501 = "Not Implemented";
	protected static final String HTML_STATUS_503 = "Service Unavailable";
}
