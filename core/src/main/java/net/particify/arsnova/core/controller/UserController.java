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

package net.particify.arsnova.core.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.LoginCredentials;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.service.UserService;
import net.particify.arsnova.core.web.exceptions.BadRequestException;
import net.particify.arsnova.core.web.exceptions.ForbiddenException;

@RestController
@EntityRequestMapping(UserController.REQUEST_MAPPING)
public class UserController extends AbstractEntityController<UserProfile> {
  protected static final String REQUEST_MAPPING = "/user";
  private static final String REGISTER_MAPPING = "/register";
  private static final String ACTIVATE_MAPPING = DEFAULT_ID_MAPPING + "/activate";
  private static final String RESET_ACTIVATE_MAPPING = DEFAULT_ID_MAPPING + "/resetactivation";
  private static final String RESET_PASSWORD_MAPPING = DEFAULT_ID_MAPPING + "/resetpassword";

  private UserService userService;

  public UserController(
      @Qualifier("securedUserService") final UserService userService) {
    super(userService);
    this.userService = userService;
  }

  static class Activation {
    private String key;

    public String getKey() {
      return key;
    }

    @JsonView(View.Public.class)
    public void setKey(final String key) {
      this.key = key;
    }
  }

  static class PasswordReset {
    private String key;
    private String password;

    public String getKey() {
      return key;
    }

    @JsonView(View.Public.class)
    public void setKey(final String key) {
      this.key = key;
    }

    public String getPassword() {
      return password;
    }

    @JsonView(View.Public.class)
    public void setPassword(final String password) {
      this.password = password;
    }
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }

  @PostMapping(REGISTER_MAPPING)
  public void register(@RequestBody final LoginCredentials loginCredentials) {
    if (userService.create(loginCredentials.getLoginId(), loginCredentials.getPassword()) == null) {
      throw new ForbiddenException();
    }
  }

  @PostMapping(ACTIVATE_MAPPING)
  public void activate(
      @PathVariable final String id,
      @RequestParam(required = false) final String key,
      final HttpServletRequest request) {
    if (key != null) {
      if (!userService.activateAccount(id, key, request.getRemoteAddr())) {
        throw new BadRequestException();
      }
    } else {
      userService.activateAccount(id);
    }
  }

  @PostMapping(RESET_ACTIVATE_MAPPING)
  public void resetActivation(
      @PathVariable final String id,
      final HttpServletRequest request) {
    userService.resetActivation(id, request.getRemoteAddr());
  }

  @PostMapping(RESET_PASSWORD_MAPPING)
  public void resetPassword(
      @PathVariable final String id,
      @RequestBody final PasswordReset passwordReset) {
    if (passwordReset.getKey() != null) {
      if (!userService.resetPassword(id, passwordReset.getKey(), passwordReset.getPassword())) {
        throw new ForbiddenException();
      }
    } else {
      userService.initiatePasswordReset(id);
    }
  }

  @Override
  protected String resolveAlias(final String alias) {
    return userService.getByUsername(alias).getId();
  }
}
