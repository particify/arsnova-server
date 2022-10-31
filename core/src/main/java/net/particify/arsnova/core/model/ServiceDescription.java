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

package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;
import java.util.stream.Collectors;

import net.particify.arsnova.core.config.properties.AuthenticationProviderProperties;
import net.particify.arsnova.core.model.serialization.View;

/**
 * A login service description. For example, this class is used to display the login buttons in ARSnova mobile.
 */
public class ServiceDescription {
  private String id;
  private String name;
  private String dialogUrl;
  private String image;
  private int order = 0;
  private Set<AuthenticationProviderProperties.Provider.Role> allowedRoles;
  private Set<String> allowedRoleStrings;

  public ServiceDescription(final String id, final String name, final String dialogUrl) {
    this.id = id;
    this.name = name;
    this.dialogUrl = dialogUrl;
  }

  public ServiceDescription(final String id, final String name, final String dialogUrl,
      final Set<AuthenticationProviderProperties.Provider.Role> allowedRoles) {
    this.id = id;
    this.name = name;
    this.dialogUrl = dialogUrl;
    setAllowedRoles(allowedRoles);
  }

  public ServiceDescription(final String id, final String name, final String dialogUrl,
      final Set<AuthenticationProviderProperties.Provider.Role> allowedRoles, final String image) {
    this.id = id;
    this.name = name;
    this.dialogUrl = dialogUrl;
    setAllowedRoles(allowedRoles);
    if (!"".equals(image)) {
      this.image = image;
    }
  }

  @JsonView(View.Public.class)
  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  @JsonView(View.Public.class)
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @JsonView(View.Public.class)
  public String getDialogUrl() {
    return dialogUrl;
  }

  public void setDialogUrl(final String dialogUrl) {
    this.dialogUrl = dialogUrl;
  }

  @JsonView(View.Public.class)
  public String getImage() {
    return image;
  }

  public void setImage(final String image) {
    this.image = image;
  }

  @JsonView(View.Public.class)
  public int getOrder() {
    return order;
  }

  public void setOrder(final int order) {
    this.order = order;
  }

  public Set<AuthenticationProviderProperties.Provider.Role> getAllowedRoles() {
    return allowedRoles;
  }

  public void setAllowedRoles(final Set<AuthenticationProviderProperties.Provider.Role> allowedRoles) {
    this.allowedRoles = allowedRoles;
    this.allowedRoleStrings = allowedRoles.stream().map(r -> {
      switch (r) {
        case MODERATOR:
          return "speaker";
        case PARTICIPANT:
          return "student";
        default:
          throw new IllegalArgumentException();
      }
    }).collect(Collectors.toSet());
  }

  @JsonView(View.Public.class)
  @JsonProperty("allowedRoles")
  public Set<String> getAllowedRolesAsStrings() {
    return allowedRoleStrings;
  }
}
