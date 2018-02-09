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

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.security.User;
import de.thm.arsnova.security.pac4j.OAuthToken;
import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.oauth.profile.google2.Google2Email;
import org.pac4j.oauth.profile.google2.Google2Profile;
import org.pac4j.oauth.profile.google2.Google2ProfileDefinition;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

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

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, TestAppConfig.class, TestPersistanceConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class UserServiceTest {

	private static final ConcurrentHashMap<UUID, UserAuthentication> socketid2user = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> user2session = new ConcurrentHashMap<>();

	@Test
	public void testSocket2UserPersistence() throws IOException, ClassNotFoundException {
		//socketid2user.put(UUID.randomUUID(), new UserAuthentication(new UsernamePasswordAuthenticationToken("ptsr00", UUID.randomUUID())));
		//socketid2user.put(UUID.randomUUID(), new UserAuthentication(new AttributePrincipalImpl("ptstr0")));

		Google2Email email = new Google2Email();
		email.setEmail("mail@host.com");
		ArrayList<Google2Email> emails = new ArrayList<>();
		emails.add(email);
		Google2Profile profile = new Google2Profile();
		profile.addAttribute(Google2ProfileDefinition.DISPLAY_NAME, "ptsr00");
		profile.addAttribute(Google2ProfileDefinition.EMAILS, emails);
		UserProfile userProfile = new UserProfile(UserProfile.AuthProvider.GOOGLE, "ptsr00");
		userProfile.setId(UUID.randomUUID().toString());
		User user = new User(userProfile, Collections.emptyList());
		OAuthToken token = new OAuthToken(user, profile, Collections.emptyList());
		socketid2user.put(UUID.randomUUID(), new UserAuthentication(token));

		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
		socketid2user.put(UUID.randomUUID(), new UserAuthentication(new AnonymousAuthenticationToken("ptsr00", UUID.randomUUID(), authorities)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream objOut = new ObjectOutputStream(out);
		objOut.writeObject(socketid2user);
		objOut.close();
		ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
		Map<UUID, UserAuthentication> actual = (Map<UUID, UserAuthentication>) objIn.readObject();
		assertEquals(actual, socketid2user);
	}

	@Test
	public void testUser2SessionPersistence() throws IOException, ClassNotFoundException {
		user2session.put("ptsr00", UUID.randomUUID().toString());
		user2session.put("ptsr01", UUID.randomUUID().toString());
		user2session.put("ptsr02", UUID.randomUUID().toString());
		user2session.put("ptsr03", UUID.randomUUID().toString());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream objOut = new ObjectOutputStream(out);
		objOut.writeObject(user2session);
		objOut.close();
		ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
		Map<String, String> actual = (Map<String, String>) objIn.readObject();
		assertEquals(actual, user2session);
	}


}
