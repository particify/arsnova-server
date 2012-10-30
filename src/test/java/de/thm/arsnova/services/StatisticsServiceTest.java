package de.thm.arsnova.services;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/test/resources/test-config.xml"
})
public class StatisticsServiceTest {

	@Autowired
	private IStatisticsService statisticsService;

	@Test
	public final void testShouldReturnCurrentActiveUsers() {
		int actual = statisticsService.countActiveUsers();
		//assertEquals(0, actual);
	}

}
