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

package net.particify.arsnova.core.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.mock.web.MockServletContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import net.particify.arsnova.core.CoreApplication;
import net.particify.arsnova.core.config.properties.MessageBrokerProperties;
import net.particify.arsnova.core.config.properties.SecurityProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.security.jwt.JwtService;
import net.particify.arsnova.core.service.StubAuthenticationService;

@TestConfiguration
@ComponentScan({
    "net.particify.arsnova.core.controller",
    "net.particify.arsnova.core.security",
    "net.particify.arsnova.core.service",
    "net.particify.arsnova.core.web",
    "net.particify.arsnova.core.websocket.handler"})
@PropertySource(
    value = {
        "classpath:config/defaults.yml",
        "classpath:config/actuator.yml",
        "classpath:config/alias-words.yml",
        "classpath:config/test.yml"
    },
    encoding = "UTF-8",
    factory = YamlPropertySourceFactory.class)
@Import({
    CoreApplication.class,
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

  @Bean("authenticationService")
  @Primary
  public StubAuthenticationService stubAuthenticationService(
      final SecurityProperties securityProperties,
      final JwtService jwtService) {
    return new StubAuthenticationService(securityProperties, jwtService);
  }

  @Bean
  @TaskExecutorConfig.RabbitConnectionExecutor
  public TaskExecutor rabbitConnectionExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("RabbitConnection");
    executor.afterPropertiesSet();
    return executor;
  }

  @Bean
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
  public RabbitTemplate rabbitTemplate(
      final ConnectionFactory connectionFactory,
      final MessageConverter messageConverter) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    return rabbitTemplate;
  }
}
