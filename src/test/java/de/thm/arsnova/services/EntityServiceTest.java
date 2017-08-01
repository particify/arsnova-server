package de.thm.arsnova.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.persistance.SessionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.AdditionalAnswers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, TestAppConfig.class, TestPersistanceConfig.class, TestSecurityConfig.class})
@ActiveProfiles("test")
public class EntityServiceTest {
	@Autowired
	@Qualifier("defaultJsonMessageConverter")
	private MappingJackson2HttpMessageConverter jackson2HttpMessageConverter;

	@Autowired
	private SessionRepository sessionRepository;

	@Test
	@WithMockUser(username="TestUser")
	public void testPatch() throws IOException {
		final ObjectMapper objectMapper = jackson2HttpMessageConverter.getObjectMapper();
		final EntityService<Session> entityService = new EntityService<>(Session.class, sessionRepository, objectMapper);

		when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());

		final String originalId = "d8833f0d78964a9487ded02ba2dfbbad";
		final String originalName = "Test Session";
		final String originalCreator = "TestUser";
		final boolean originalActive = false;
		final Session session = new Session();
		session.setId(originalId);
		session.setName(originalName);
		session.setActive(originalActive);
		session.setCreator(originalCreator);

		final String patchedName = "Patched Session";
		final boolean patchedActive = true;
		final Map<String, Object> patchedValues = new HashMap<>();
		patchedValues.put("name", patchedName);
		patchedValues.put("active", patchedActive);
		patchedValues.put("creator", "Should not be changeable.");

		entityService.patch(session, patchedValues);

		assertEquals(originalId, session.getId());
		assertEquals(patchedName, session.getName());
		assertEquals(patchedActive, session.isActive());
		assertEquals(originalCreator, session.getCreator());
	}
}
