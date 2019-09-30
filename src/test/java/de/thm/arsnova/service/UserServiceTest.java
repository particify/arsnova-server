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

package de.thm.arsnova.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.oidc.profile.google.GoogleOidcProfile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.config.WebSocketConfig;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.migration.v2.ClientAuthentication;
import de.thm.arsnova.security.User;
import de.thm.arsnova.security.pac4j.SsoAuthenticationToken;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AppConfig.class,
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class,
		WebSocketConfig.class})
@ActiveProfiles("test")
public class UserServiceTest {

	private static final ConcurrentHashMap<UUID, ClientAuthentication> socketid2user = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> user2session = new ConcurrentHashMap<>();

	@Test
	public void testSocket2UserPersistence() throws IOException, ClassNotFoundException {
		//socketid2user.put(UUID.randomUUID(),
		//		new ClientAuthentication(new UsernamePasswordAuthenticationToken("ptsr00", UUID.randomUUID())));
		//socketid2user.put(UUID.randomUUID(), new ClientAuthentication(new AttributePrincipalImpl("ptstr0")));

		final GoogleOidcProfile profile = new GoogleOidcProfile();
		profile.addAttribute(CommonProfileDefinition.DISPLAY_NAME, "ptsr00");
		profile.addAttribute(CommonProfileDefinition.EMAIL, "mail@host.com");
		profile.addAttribute("email_verified", true);
		final UserProfile userProfile = new UserProfile(UserProfile.AuthProvider.GOOGLE, "ptsr00");
		userProfile.setId(UUID.randomUUID().toString());
		final User user = new User(userProfile, Collections.emptyList());
		final SsoAuthenticationToken token = new SsoAuthenticationToken(user, profile, Collections.emptyList());
		socketid2user.put(UUID.randomUUID(), new ClientAuthentication(token));

		final List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
		socketid2user.put(UUID.randomUUID(), new ClientAuthentication(
				new AnonymousAuthenticationToken("ptsr00", UUID.randomUUID(), authorities)));

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ObjectOutputStream objOut = new ObjectOutputStream(out);
		objOut.writeObject(socketid2user);
		objOut.close();
		final ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
		final Map<UUID, ClientAuthentication> actual = (Map<UUID, ClientAuthentication>) objIn.readObject();
		assertEquals(actual, socketid2user);
	}

	@Test
	public void testUser2SessionPersistence() throws IOException, ClassNotFoundException {
		user2session.put("ptsr00", UUID.randomUUID().toString());
		user2session.put("ptsr01", UUID.randomUUID().toString());
		user2session.put("ptsr02", UUID.randomUUID().toString());
		user2session.put("ptsr03", UUID.randomUUID().toString());

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ObjectOutputStream objOut = new ObjectOutputStream(out);
		objOut.writeObject(user2session);
		objOut.close();
		final ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
		final Map<String, String> actual = (Map<String, String>) objIn.readObject();
		assertEquals(actual, user2session);
	}


}
