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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(MessageBrokerProperties.PREFIX)
public class MessageBrokerProperties {
  public static final String PREFIX = SystemProperties.PREFIX + ".message-broker";

  public abstract static class Server {
    private boolean enabled;
    private String host;
    private int port;
    private String username;
    private String password;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
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

  public static class Rabbitmq extends Server {
    private String virtualHost;
    private boolean manageDeclarations;
    private Listener listener;

    public String getVirtualHost() {
      return virtualHost;
    }

    public void setVirtualHost(final String virtualHost) {
      this.virtualHost = virtualHost;
    }

    public boolean isManageDeclarations() {
      return manageDeclarations;
    }

    public void setManageDeclarations(final boolean manageDeclarations) {
      this.manageDeclarations = manageDeclarations;
    }

    public Listener getListener() {
      return listener;
    }

    public void setListener(final Listener listener) {
      this.listener = listener;
    }
  }

  public static class Listener {
    private int maxAttempts;

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(final int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }
  }

  public static class PublishedEvent {
    public String entityType;
    public String eventType;
    public Set<String> includedProperties = new HashSet<>();

    public String getEntityType() {
      return entityType;
    }

    public void setEntityType(final String entityType) {
      this.entityType = entityType;
    }

    public String getEventType() {
      return eventType;
    }

    public void setEventType(final String eventType) {
      this.eventType = eventType;
    }

    public Set<String> getIncludedProperties() {
      return includedProperties;
    }

    public void setIncludedProperties(final Set<String> includedProperties) {
      this.includedProperties = includedProperties;
    }
  }

  private Rabbitmq rabbitmq;
  private List<PublishedEvent> publishedEvents;

  public Rabbitmq getRabbitmq() {
    return rabbitmq;
  }

  public void setRabbitmq(final Rabbitmq rabbitmq) {
    this.rabbitmq = rabbitmq;
  }

  public List<PublishedEvent> getPublishedEvents() {
    return publishedEvents;
  }

  public void setPublishedEvents(final List<PublishedEvent> publishedEvents) {
    this.publishedEvents = publishedEvents;
  }
}
