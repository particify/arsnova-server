package de.thm.arsnova.services;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.scribe.up.profile.google.Google2AttributesDefinition;
import org.scribe.up.profile.google.Google2Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import de.thm.arsnova.entities.User;

@RunWith(BlockJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class UserServiceTest {

	private static final ConcurrentHashMap<UUID, User> socketid2user = new ConcurrentHashMap<UUID, User>();
	private static final ConcurrentHashMap<String, String> user2session = new ConcurrentHashMap<String, String>();

	@Test
	public void testSocket2UserPersistence() throws IOException, ClassNotFoundException {
		socketid2user.put(UUID.randomUUID(), new User(new UsernamePasswordAuthenticationToken("ptsr00", UUID.randomUUID())));
		socketid2user.put(UUID.randomUUID(), new User(new AttributePrincipalImpl("ptstr0")));

		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(Google2AttributesDefinition.EMAIL, "mail@host.com");
		Google2Profile profile = new Google2Profile("ptsr00", attributes);

		socketid2user.put(UUID.randomUUID(), new User(profile));
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
		socketid2user.put(UUID.randomUUID(), new User(new AnonymousAuthenticationToken("ptsr00", UUID.randomUUID(), authorities)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream objOut = new ObjectOutputStream(out);
		objOut.writeObject(socketid2user);
		objOut.close();
		ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
		Map<UUID, User> actual = (Map<UUID, User>) objIn.readObject();
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
