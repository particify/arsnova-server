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

package net.particify.arsnova.core.config.properties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties(SystemProperties.PREFIX)
public class SystemProperties {
  public static final String PREFIX = "system";

  public static class Api {
    private String proxyPath;
    private boolean indentResponseBody;
    private boolean exposeErrorMessages;
    private boolean forwardAliases;

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

    public boolean isForwardAliases() {
      return forwardAliases;
    }

    public void setForwardAliases(final boolean forwardAliases) {
      this.forwardAliases = forwardAliases;
    }
  }

  public static class Mail {
    private String senderName;
    private String senderAddress;
    private String host;
    private int port;
    private boolean implicitTls;
    private String username;
    private String password;
    private String localhost;

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

    public int getPort() {
      return port;
    }

    public void setPort(final int port) {
      this.port = port;
    }

    public boolean getImplicitTls() {
      return implicitTls;
    }

    public void setImplicitTls(final boolean implicitTls) {
      this.implicitTls = implicitTls;
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

    public String getLocalhost() {
      return localhost;
    }

    public void setLocalhost(final String localhost) {
      this.localhost = localhost;
    }
  }

  public static class Caching {
    private long maxEntries;

    @DurationUnit(ChronoUnit.MINUTES)
    private Duration expiry;

    public long getMaxEntries() {
      return maxEntries;
    }

    public void setMaxEntries(final long maxEntries) {
      this.maxEntries = maxEntries;
    }

    public Duration getExpiry() {
      return expiry;
    }

    public void setExpiry(final Duration expiry) {
      this.expiry = expiry;
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

  public static class AutoDeletionThresholds {
    @DurationUnit(ChronoUnit.DAYS)
    private Duration userInactivityPeriod;

    private int userInactivityLimit;

    public Duration getUserInactivityPeriod() {
      return userInactivityPeriod;
    }

    public void setUserInactivityPeriod(final Duration userInactivityPeriod) {
      this.userInactivityPeriod = userInactivityPeriod;
    }

    public int getUserInactivityLimit() {
      return userInactivityLimit;
    }

    public void setUserInactivityLimit(final int userInactivityLimit) {
      this.userInactivityLimit = userInactivityLimit;
    }
  }

  private String rootUrl;
  private boolean externalUserManagement;
  private Api api;
  private Mail mail;
  private Caching caching;
  private String authzServiceUrl;
  private FormattingService formattingService;
  private LmsConnector lmsConnector;
  private AutoDeletionThresholds autoDeletionThresholds;

  public String getRootUrl() {
    return rootUrl;
  }

  public void setRootUrl(final String rootUrl) {
    this.rootUrl = rootUrl;
  }

  public boolean isExternalUserManagement() {
    return externalUserManagement;
  }

  public void setExternalUserManagement(final boolean externalUserManagement) {
    this.externalUserManagement = externalUserManagement;
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

  public Caching getCaching() {
    return caching;
  }

  public void setCaching(final Caching caching) {
    this.caching = caching;
  }

  public String getAuthzServiceUrl() {
    return authzServiceUrl;
  }

  public void setAuthzServiceUrl(final String authzServiceUrl) {
    this.authzServiceUrl = authzServiceUrl;
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

  public AutoDeletionThresholds getAutoDeletionThresholds() {
    return autoDeletionThresholds;
  }

  public void setAutoDeletionThresholds(final AutoDeletionThresholds autoDeletionThresholds) {
    this.autoDeletionThresholds = autoDeletionThresholds;
  }
}
