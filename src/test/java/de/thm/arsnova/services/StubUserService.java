/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.persistance.UserRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class StubUserService extends UserServiceImpl {

	private UserAuthentication stubUser = null;

	public StubUserService(UserRepository repository, JavaMailSender mailSender) {
		super(repository, mailSender);
	}

	public void setUserAuthenticated(boolean isAuthenticated) {
		this.setUserAuthenticated(isAuthenticated, "ptsr00");
	}

	public void setUserAuthenticated(boolean isAuthenticated, String username) {
		if (isAuthenticated) {
			stubUser = new UserAuthentication(new UsernamePasswordAuthenticationToken(username, "testpassword"));
			return;
		}
		stubUser = null;
	}

	public void useAnonymousUser() {
		stubUser = new UserAuthentication(new UsernamePasswordAuthenticationToken("anonymous", ""));
	}

	@Override
	public UserAuthentication getCurrentUser() {
		return stubUser;
	}

	public void setRole(UserRoomService.Role role) {
		stubUser.setRole(role);
	}
}
