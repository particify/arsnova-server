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

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;

import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.model.serialization.View;

public class AuthenticationProvider {
  public enum Type {
    ANONYMOUS,
    USERNAME_PASSWORD,
    SSO
  }

  private String id;
  private boolean enabled;
  private String title;
  private int order;
  private Set<AuthenticationProviderProperties.Provider.Role> allowedRoles;
  private Type type;

  public AuthenticationProvider(final String id, final AuthenticationProviderProperties.Provider provider) {
    this.id = id;
    this.enabled = provider.isEnabled();
    this.title = provider.getTitle();
    this.order = provider.getOrder();
    this.allowedRoles = provider.getAllowedRoles();

    if (provider instanceof AuthenticationProviderProperties.Guest) {
      type = Type.ANONYMOUS;
    } else if (provider instanceof AuthenticationProviderProperties.Registered
        || provider instanceof AuthenticationProviderProperties.Ldap) {
      type = Type.USERNAME_PASSWORD;
    } else {
      type = Type.SSO;
    }
  }

  @JsonView(View.Public.class)
  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  @JsonView(View.Public.class)
  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  @JsonView(View.Public.class)
  public int getOrder() {
    return order;
  }

  public void setOrder(final int order) {
    this.order = order;
  }

  @JsonView(View.Public.class)
  public Set<AuthenticationProviderProperties.Provider.Role> getAllowedRoles() {
    return allowedRoles;
  }

  public void setAllowedRoles(final Set<AuthenticationProviderProperties.Provider.Role> allowedRoles) {
    this.allowedRoles = allowedRoles;
  }

  @JsonView(View.Public.class)
  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }
}
