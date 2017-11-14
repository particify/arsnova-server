package de.thm.arsnova.services;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import org.junit.Test;
import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, TestAppConfig.class, TestPersistanceConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class UserSessionServiceImplSerializationTest {

	@Test
	public void serializationTest() throws NotSerializableException {
		User user = new User(new UsernamePasswordAuthenticationToken("ptsr00", UUID.randomUUID()));
		Session session = new Session();
		session.setId("TestSession");
		UserSessionService.Role role = UserSessionService.Role.STUDENT;

		UserSessionServiceImpl ussi1 = new UserSessionServiceImpl();
		ussi1.setUser(user);
		ussi1.setSession(session);
		ussi1.setSocketId(UUID.randomUUID());
		ussi1.setRole(role);

		Exception ex = null;
		try {
			FileOutputStream fileOut =
					new FileOutputStream("UserSessionServiceImpl.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(ussi1);
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in UserSessionServiceImpl.ser");
		} catch (IOException e) {
			ex = e;
		}

		assertEquals(null, ex);

		UserSessionServiceImpl readedUSSI1 = null;
		try {
			FileInputStream fileIn = new FileInputStream("UserSessionServiceImpl.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			readedUSSI1 = (UserSessionServiceImpl) in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException i) {
			i.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			ex = e;
		}

		assertEquals(null, ex);

		assertEquals(ussi1.getUser(), readedUSSI1.getUser());
		assertEquals(ussi1.getRole(), readedUSSI1.getRole());
		assertEquals(null, readedUSSI1.getSession());
		assertEquals(ussi1.getSocketId(), readedUSSI1.getSocketId());
	}
}
