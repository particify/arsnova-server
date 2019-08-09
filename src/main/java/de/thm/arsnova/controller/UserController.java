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

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.LoginCredentials;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.service.RoomService;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.ForbiddenException;

@RestController
@RequestMapping(UserController.REQUEST_MAPPING)
public class UserController extends AbstractEntityController<UserProfile> {
	protected static final String REQUEST_MAPPING = "/user";
	private static final String REGISTER_MAPPING = "/register";
	private static final String ACTIVATE_MAPPING = DEFAULT_ID_MAPPING + "/activate";
	private static final String RESET_PASSWORD_MAPPING = DEFAULT_ID_MAPPING + "/resetpassword";
	private static final String ROOM_HISTORY_MAPPING = DEFAULT_ID_MAPPING + "/roomHistory";

	private UserService userService;
	private RoomService roomService;

	public UserController(final UserService userService, final RoomService roomService) {
		super(userService);
		this.userService = userService;
		this.roomService = roomService;
	}

	class Activation {
		private String key;

		public String getKey() {
			return key;
		}

		@JsonView(View.Public.class)
		public void setKey(final String key) {
			this.key = key;
		}
	}

	class PasswordReset {
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
			@RequestParam final String key) {
		final UserProfile userProfile = userService.get(id, true);
		if (userProfile == null || !key.equals(userProfile.getAccount().getActivationKey())) {
			throw new BadRequestException();
		}
		userProfile.getAccount().setActivationKey(null);
		userService.update(userProfile);
	}

	@PostMapping(RESET_PASSWORD_MAPPING)
	public void resetPassword(
			@PathVariable final String id,
			@RequestBody final PasswordReset passwordReset) {
		final UserProfile userProfile = userService.get(id, true);
		if (userProfile == null) {
			throw new BadRequestException();
		}

		if (passwordReset.getKey() != null) {
			if (!userService.resetPassword(userProfile, passwordReset.getKey(), passwordReset.getPassword())) {
				throw new ForbiddenException();
			}
		} else {
			userService.initiatePasswordReset(id);
		}
	}

	@PostMapping(ROOM_HISTORY_MAPPING)
	public void postRoomHistoryEntry(@PathVariable final String id,
			@RequestBody final UserProfile.RoomHistoryEntry roomHistoryEntry) {
		userService.addRoomToHistory(userService.get(id), roomService.get(roomHistoryEntry.getRoomId()));
	}

	@Override
	protected String resolveAlias(final String alias) {
		return userService.getByUsername(alias).getId();
	}
}
