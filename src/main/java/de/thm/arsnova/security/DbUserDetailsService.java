/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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

	public static final Logger LOGGER = LoggerFactory
			.getLogger(DbUserDetailsService.class);

	@Override
	public UserDetails loadUserByUsername(String username) {
		LOGGER.debug("Load user: " + username);
		DbUser dbUser = dao.getUser(username);
		if (null == dbUser) {
			throw new UsernameNotFoundException("User does not exist.");
		}

		final List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_DB_USER"));

		return new User(username, dbUser.getPassword(),
				null == dbUser.getActivationKey(), true, true, true,
				grantedAuthorities);
	}
}
