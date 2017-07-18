package de.thm.arsnova.config;

import de.thm.arsnova.services.StubUserService;
import de.thm.arsnova.websocket.ArsnovaSocketioServer;
import de.thm.arsnova.websocket.ArsnovaSocketioServerImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ComponentScan({
		"de.thm.arsnova.aop",
		"de.thm.arsnova.cache",
		"de.thm.arsnova.controller",
		"de.thm.arsnova.dao",
		"de.thm.arsnova.events",
		"de.thm.arsnova.security",
		"de.thm.arsnova.services",
		"de.thm.arsnova.web"})
@Configuration
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@EnableCaching(mode = AdviceMode.ASPECTJ)
@EnableSpringConfigured
@EnableWebMvc
@PropertySource(
		value = {"classpath:arsnova.properties.example", "file:/etc/arsnova/arsnova.properties"},
		ignoreResourceNotFound = true
)
@Profile("test")
public class TestAppConfig {
	private static int testPortOffset = 0;

	@Value("${socketio.bind-address}") private String socketAddress;
	@Value("${socketio.port}") private int socketPort;

	@Bean
	public MockServletContext servletContext() {
		return new MockServletContext();
	}

	@Bean
	public CustomScopeConfigurer customScopeConfigurer() {
		final CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("session", new SimpleThreadScope());

		return configurer;
	}

	@Bean(name = "socketServer", initMethod = "startServer", destroyMethod = "stopServer")
	public ArsnovaSocketioServer socketTestServer() {
		final int testSocketPort = 1234 + testPortOffset++ % 10;
		final ArsnovaSocketioServerImpl socketServer = new ArsnovaSocketioServerImpl();
		socketServer.setHostIp(socketAddress);
		socketServer.setPortNumber(socketPort + testSocketPort);

		return socketServer;
	}

	@Bean
	@Primary
	public StubUserService stubUserService() {
		return new StubUserService();
	}
}
