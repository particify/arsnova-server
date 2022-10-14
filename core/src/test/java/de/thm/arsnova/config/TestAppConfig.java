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

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.Validator;

import de.thm.arsnova.ArsnovaApplication;
import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.persistence.UserRepository;
import de.thm.arsnova.security.PasswordUtils;
import de.thm.arsnova.service.EmailService;
import de.thm.arsnova.service.StubUserService;

@TestConfiguration
@ComponentScan({
    "de.thm.arsnova.controller",
    "de.thm.arsnova.security",
    "de.thm.arsnova.service",
    "de.thm.arsnova.web",
    "de.thm.arsnova.websocket.handler"})
@PropertySource(
    value = {
        "classpath:config/defaults.yml",
        "classpath:config/actuator.yml",
        "classpath:config/test.yml"
    },
    encoding = "UTF-8",
    factory = YamlPropertySourceFactory.class)
@Import({
    ArsnovaApplication.class,
    AppConfig.class})
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

  @Bean("userServiceImpl")
  @Primary
  public StubUserService stubUserService(
      final UserRepository repository,
      final SystemProperties systemProperties,
      final SecurityProperties securityProperties,
      final AuthenticationProviderProperties authenticationProviderProperties,
      final EmailService emailService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator,
      final PasswordUtils passwordUtils) {
    return new StubUserService(repository, systemProperties, securityProperties, authenticationProviderProperties,
        emailService, jackson2HttpMessageConverter, validator, passwordUtils);
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
      final MessageBrokerProperties messageBrokerProperties) {
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
