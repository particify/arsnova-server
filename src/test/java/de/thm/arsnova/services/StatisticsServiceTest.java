package de.thm.arsnova.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Statistics;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-security.xml",
		"file:src/test/resources/test-config.xml"
})
@ActiveProfiles("test")
public class StatisticsServiceTest {

	@InjectMocks
	private final IStatisticsService statisticsService = new StatisticsService();

	@Mock
	private IDatabaseDao databaseDao;

	@Before
	public final void startup() {
		MockitoAnnotations.initMocks(this);
		when(databaseDao.countQuestions()).thenReturn(123);
		when(databaseDao.countActiveUsers(anyInt())).thenReturn(42);
		when(databaseDao.countOpenSessions()).thenReturn(1978);
		when(databaseDao.countClosedSessions()).thenReturn(1984);
		when(databaseDao.countAnswers()).thenReturn(2014);
	}

	@After
	public final void cleanup() {
	}

	@Test
	public final void testShouldReturnEqualStatistics() {
		final Statistics actual = statisticsService.getStatistics();

		final Statistics expected = new Statistics();
		expected.setActiveUsers(42);
		expected.setAnswers(2014);
		expected.setClosedSessions(1984);
		expected.setOpenSessions(1978);
		expected.setQuestions(123);
		expected.setLoggedinUsers(0);

		assertEquals(expected, actual);
	}
}
