package de.thm.arsnova.services;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.Statistics;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml"
})
public class StatisticsServiceTest {

	@Autowired
	private IStatisticsService statisticsService;

	@Autowired
	private StubUserService userService;

	@Autowired
	private StubDatabaseDao databaseDao;

	@Before
	public final void startup() {
		// Create new session to be appended to the existing two sessions
		Session session = new Session();
		session.setKeyword("1111222");
		session.setActive(false);
		databaseDao.saveSession(session);
	}
	
	@After
	public final void cleanup() {
		databaseDao.cleanupTestData();
	}
	
	@Test
	public final void testShouldReturnNoActiveUsers() {
		int actual = statisticsService.countActiveUsers();
		assertEquals(0, actual);
	}

	@Test
	public final void testShouldReturnCurrentActiveUsers() {
		Session session = new Session();
		session.setKeyword("1278127812");
		
		userService.setUserAuthenticated(true);
		databaseDao.registerAsOnlineUser(userService.getCurrentUser(), session);

		int actual = statisticsService.countActiveUsers();
		assertEquals(1, actual);
		userService.setUserAuthenticated(false);
	}
	
	@Test
	public final void testShouldReturnStatistics() {
		Statistics actual = statisticsService.getStatistics();
		assertEquals(2, actual.getOpenSessions());
		assertEquals(1, actual.getClosedSessions());
	}
}
