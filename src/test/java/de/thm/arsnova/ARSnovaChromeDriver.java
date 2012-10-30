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

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.html5.*;
import org.openqa.selenium.remote.html5.*;

public class ARSnovaChromeDriver extends ChromeDriver implements WebStorage {

	public LocalStorage getLocalStorage() {
		return new RemoteLocalStorage(this.getExecuteMethod());
	}

	@Override
	public SessionStorage getSessionStorage() {
		return new RemoteSessionStorage(this.getExecuteMethod());
	}
}
