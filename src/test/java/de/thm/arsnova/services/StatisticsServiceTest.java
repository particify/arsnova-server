package de.thm.arsnova.services;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.thm.arsnova.dao.StubDatabaseDao;
import de.thm.arsnova.entities.Session;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
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

	@Test
	public final void testShouldReturnNoActiveUsers() {
		int actual = statisticsService.countActiveUsers();
		assertEquals(0, actual);
	}

	@Test
	public final void testShouldReturnCurrentActiveUsers() {
		Session session = new Session();
		session.setKeyword("1278127812");
		databaseDao.registerAsOnlineUser(userService.getCurrentUser(), session);

		int actual = statisticsService.countActiveUsers();
		assertEquals(1, actual);
	}
}
