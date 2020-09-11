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

package de.thm.arsnova.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(SystemProperties.PREFIX)
public class SystemProperties {
	public static final String PREFIX = "system";

	public static class Api {
		private String proxyPath;
		private boolean indentResponseBody;
		private boolean exposeErrorMessages;

		public String getProxyPath() {
			return proxyPath;
		}

		public void setProxyPath(final String proxyPath) {
			this.proxyPath = proxyPath;
		}

		public boolean isIndentResponseBody() {
			return indentResponseBody;
		}

		public void setIndentResponseBody(final boolean indentResponseBody) {
			this.indentResponseBody = indentResponseBody;
		}

		public boolean isExposeErrorMessages() {
			return exposeErrorMessages;
		}

		public void setExposeErrorMessages(final boolean exposeErrorMessages) {
			this.exposeErrorMessages = exposeErrorMessages;
		}
	}

	public static class Mail {
		private String senderName;
		private String senderAddress;
		private String host;

		public String getSenderName() {
			return senderName;
		}

		public void setSenderName(final String senderName) {
			this.senderName = senderName;
		}

		public String getSenderAddress() {
			return senderAddress;
		}

		public void setSenderAddress(final String senderAddress) {
			this.senderAddress = senderAddress;
		}

		public String getHost() {
			return host;
		}

		public void setHost(final String host) {
			this.host = host;
		}
	}

	public static class FormattingService {
		private boolean enabled;
		private String hostUrl;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}

		public String getHostUrl() {
			return hostUrl;
		}

		public void setHostUrl(final String hostUrl) {
			this.hostUrl = hostUrl;
		}
	}

	public static class LmsConnector {
		private boolean enabled;
		private String hostUrl;
		private String username;
		private String password;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}

		public String getHostUrl() {
			return hostUrl;
		}

		public void setHostUrl(final String hostUrl) {
			this.hostUrl = hostUrl;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(final String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(final String password) {
			this.password = password;
		}
	}

	public static class Socketio {
		private String bindAddress;
		private int port;
		private String proxyPath;

		public String getBindAddress() {
			return bindAddress;
		}

		public void setBindAddress(final String bindAddress) {
			this.bindAddress = bindAddress;
		}

		public int getPort() {
			return port;
		}

		public void setPort(final int port) {
			this.port = port;
		}

		public String getProxyPath() {
			return proxyPath;
		}

		public void setProxyPath(final String proxyPath) {
			this.proxyPath = proxyPath;
		}
	}

	private String rootUrl;
	private Api api;
	private Mail mail;
	private FormattingService formattingService;
	private LmsConnector lmsConnector;
	private Socketio socketio;

	public String getRootUrl() {
		return rootUrl;
	}

	public void setRootUrl(final String rootUrl) {
		this.rootUrl = rootUrl;
	}

	public Api getApi() {
		return api;
	}

	public void setApi(final Api api) {
		this.api = api;
	}

	public Mail getMail() {
		return mail;
	}

	public void setMail(final Mail mail) {
		this.mail = mail;
	}

	public FormattingService getFormattingService() {
		return formattingService;
	}

	public void setFormattingService(final FormattingService formattingService) {
		this.formattingService = formattingService;
	}

	public LmsConnector getLmsConnector() {
		return lmsConnector;
	}

	public void setLmsConnector(final LmsConnector lmsConnector) {
		this.lmsConnector = lmsConnector;
	}

	public Socketio getSocketio() {
		return socketio;
	}

	public void setSocketio(final Socketio socketio) {
		this.socketio = socketio;
	}
}
