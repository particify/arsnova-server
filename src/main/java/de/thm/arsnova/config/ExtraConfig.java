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

import de.thm.arsnova.ImageUtils;
import de.thm.arsnova.socket.ARSnovaSocket;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;
import de.thm.arsnova.web.CorsFilter;
import net.particify.arsnova.connector.client.ConnectorClient;
import net.particify.arsnova.connector.client.ConnectorClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Properties;

/**
 * Loads property file and configures non-security related beans and components.
 */
@EnableWebMvc
@Configuration
@EnableCaching
public class ExtraConfig extends WebMvcConfigurerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ExtraConfig.class);

	@Autowired
	private Environment env;

	@Resource(name = "versionInfoProperties")
	private Properties versionInfoProperties;

	@Value(value = "${connector.enable}") private boolean connectorEnable;
	@Value(value = "${connector.uri}") private String connectorUri;
	@Value(value = "${connector.username}") private String connectorUsername;
	@Value(value = "${connector.password}") private String connectorPassword;

	@Value(value = "${socketio.bind-address}") private String socketAddress;
	@Value(value = "${socketio.port}") private int socketPort;
	@Value(value = "${socketio.ssl.jks-file:}") private String socketKeystore;
	@Value(value = "${socketio.ssl.jks-password:}") private String socketKeystorePassword;
	@Value(value = "${security.cors.origins:}") private String[] corsOrigins;

	private static int testPortOffset = 0;

	@PostConstruct
	public void init() {
		logger.info("ARSnova Backend version: {} ({} {}{})",
				versionInfoProperties.getProperty("version.string"),
				versionInfoProperties.getProperty("version.build-time"),
				versionInfoProperties.getProperty("version.git.commit-id"),
				Boolean.parseBoolean(versionInfoProperties.getProperty("version.git.dirty")) ? " [dirty]" : "");
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setLocations(
			new ClassPathResource("arsnova.properties.example"),
			new FileSystemResource("/etc/arsnova/arsnova.properties")
		);
		configurer.setIgnoreResourceNotFound(true);
		configurer.setIgnoreUnresolvablePlaceholders(false);
		return configurer;
	}

	@Bean
	public PropertiesFactoryBean versionInfoProperties() {
		PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean.setLocation(new ClassPathResource("version.properties"));

		return propertiesFactoryBean;
	}

	@Bean
	public CorsFilter corsFilter() {
		return new CorsFilter(Arrays.asList(corsOrigins));
	}

	@Bean(name = "connectorClient")
	public ConnectorClient connectorClient() {
		if (!connectorEnable) {
			return null;
		}

		final ConnectorClientImpl connectorClient = new ConnectorClientImpl();
		connectorClient.setServiceLocation(connectorUri);
		connectorClient.setUsername(connectorUsername);
		connectorClient.setPassword(connectorPassword);
		return connectorClient;
	}

	@Profile("!test")
	@Bean(name = "socketServer", initMethod = "startServer", destroyMethod = "stopServer")
	public ARSnovaSocket socketServer() {
		final ARSnovaSocketIOServer socketServer = new ARSnovaSocketIOServer();
		socketServer.setHostIp(socketAddress);
		socketServer.setPortNumber(socketPort);
		socketServer.setUseSSL(!socketKeystore.isEmpty());
		socketServer.setKeystore(socketKeystore);
		socketServer.setStorepass(socketKeystorePassword);
		return socketServer;
	}

	@Profile("test")
	@Bean(name = "socketServer", initMethod = "startServer", destroyMethod = "stopServer")
	public ARSnovaSocket socketTestServer() {
		final int testSocketPort = 1234 + testPortOffset++ % 10;
		final ARSnovaSocketIOServer socketServer = new ARSnovaSocketIOServer();
		socketServer.setHostIp(socketAddress);
		socketServer.setPortNumber(socketPort + testSocketPort);
		socketServer.setUseSSL(!socketKeystore.isEmpty());
		socketServer.setKeystore(socketKeystore);
		socketServer.setStorepass(socketKeystorePassword);
		return socketServer;
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

	@Bean
	public ImageUtils imageUtils() {
		return new ImageUtils();
	}
}
