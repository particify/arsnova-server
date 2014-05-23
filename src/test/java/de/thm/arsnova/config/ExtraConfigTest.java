package de.thm.arsnova.config;

import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.thm.arsnova.connector.client.ConnectorClient;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {
		"file:src/main/webapp/WEB-INF/spring/arsnova-servlet.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-main.xml",
		"file:src/main/webapp/WEB-INF/spring/spring-security.xml",
		"file:src/test/resources/test-config.xml",
		"file:src/test/resources/test-socketioconfig.xml"
})
public class ExtraConfigTest {

	@Autowired(required = false)
	private ConnectorClient connectorClient;

	@Test
	public void testShouldNotLoadConnectorClientByDefault() {
		assertNull(connectorClient);
	}

}
