/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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
package de.thm.arsnova.security;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.services.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to load a user based on the username.
 */
@Service
public class DbUserDetailsService implements UserDetailsService {
	@Autowired
	private IDatabaseDao dao;

	@Autowired
	private IUserService userService;

	private static final Logger logger = LoggerFactory
			.getLogger(DbUserDetailsService.class);

	@Override
	public UserDetails loadUserByUsername(String username) {
		String uid = username.toLowerCase();
		logger.debug("Load user: " + uid);
		DbUser dbUser = dao.getUser(uid);
		if (null == dbUser) {
			throw new UsernameNotFoundException("User does not exist.");
		}

		final List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_DB_USER"));
		if (userService.isAdmin(uid)) {
			grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}

		return new User(uid, dbUser.getPassword(),
				null == dbUser.getActivationKey(), true, true, true,
				grantedAuthorities);
	}
}
