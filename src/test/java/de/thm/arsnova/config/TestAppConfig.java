/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.config;

import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.controller.JsonViewControllerAdviceTest;
import de.thm.arsnova.service.EntityService;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockServletContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import de.thm.arsnova.persistence.UserRepository;
import de.thm.arsnova.service.StubUserService;
import de.thm.arsnova.websocket.ArsnovaSocketioServer;
import de.thm.arsnova.websocket.ArsnovaSocketioServerImpl;

@ComponentScan({
		"de.thm.arsnova.aop",
		"de.thm.arsnova.cache",
		"de.thm.arsnova.controller",
		"de.thm.arsnova.dao",
		"de.thm.arsnova.events",
		"de.thm.arsnova.security",
		"de.thm.arsnova.services",
		"de.thm.arsnova.web",
		"de.thm.arsnova.websocket.handler"})
@Configuration
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@EnableCaching(mode = AdviceMode.ASPECTJ)
@EnableSpringConfigured
@EnableWebMvc
@EnableConfigurationProperties(MessageBrokerProperties.class)
@PropertySource(
	value = "classpath:config/test.yml",
	encoding = "UTF-8",
	factory = YamlPropertySourceFactory.class
)
@Profile("test")
public class TestAppConfig {
	private static int testPortOffset = 0;

	@Autowired
	private SystemProperties systemProperties;

	@Bean
	public MockServletContext servletContext() {
		return new MockServletContext();
	}

	@Bean
	public static CustomScopeConfigurer customScopeConfigurer() {
		final CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("session", new SimpleThreadScope());

		return configurer;
	}

	@Bean(name = "socketServer", initMethod = "startServer", destroyMethod = "stopServer")
	public ArsnovaSocketioServer socketTestServer() {
		final int testSocketPort = 1234 + testPortOffset++ % 10;
		final ArsnovaSocketioServerImpl socketServer = new ArsnovaSocketioServerImpl();
		socketServer.setHostIp(systemProperties.getSocketio().getBindAddress());
		socketServer.setPortNumber(systemProperties.getSocketio().getPort() + testSocketPort);

		return socketServer;
	}

	@Bean
	@Primary
	public StubUserService stubUserService(
			final UserRepository repository,
			final SystemProperties systemProperties,
			final SecurityProperties securityProperties,
			final AuthenticationProviderProperties authenticationProviderProperties,
			final JavaMailSender mailSender,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator) {
		return new StubUserService(repository, systemProperties, securityProperties, authenticationProviderProperties,
				mailSender, jackson2HttpMessageConverter, validator);
	}

	@Bean
	public EntityService<JsonViewControllerAdviceTest.DummyEntity> dummyEntityService() {
		return Mockito.mock(EntityService.class);
	}

	@Bean
	@Autowired
	@TaskExecutorConfig.RabbitConnectionExecutor
	public TaskExecutor rabbitConnectionExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("RabbitConnection");
		executor.afterPropertiesSet();
		return executor;
	}

	@Bean
	@Autowired
	public ConnectionFactory connectionFactory(
		@TaskExecutorConfig.RabbitConnectionExecutor final TaskExecutor executor,
		final MessageBrokerProperties messageBrokerProperties
	) {
		final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
			messageBrokerProperties.getRabbitmq().getHost(),
			messageBrokerProperties.getRabbitmq().getPort());
		connectionFactory.setUsername(messageBrokerProperties.getRabbitmq().getUsername());
		connectionFactory.setPassword(messageBrokerProperties.getRabbitmq().getPassword());
		connectionFactory.setVirtualHost(messageBrokerProperties.getRabbitmq().getVirtualHost());
		connectionFactory.setExecutor(executor);

		return connectionFactory;
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	@Autowired
	public RabbitTemplate rabbitTemplate(
			final ConnectionFactory connectionFactory,
			final MessageConverter messageConverter) {
		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		return rabbitTemplate;
	}
}
