package de.thm.arsnova.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.core.io.Resource;

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.client.ConnectorClientImpl;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Configuration
@EnableCaching
public class ExtraConfig {

	@Autowired
	private Environment env;

	@Value(value = "${connector.enable}") private boolean connectorEnable;
	@Value(value = "${connector.uri}") private String connectorUri;
	@Value(value = "${connector.username}") private String connectorUsername;
	@Value(value = "${connector.password}") private String connectorPassword;

	@Value(value = "${socketio.ip}") private String socketIp;
	@Value(value = "${socketio.port}") private int socketPort;
	@Value(value = "${security.ssl}") private boolean socketUseSll;
	@Value(value = "${security.keystore}") private String socketKeystore;
	@Value(value = "${security.storepass}") private String socketStorepass;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setLocations(new Resource[] {
				new ClassPathResource("arsnova.properties.example"),
				new FileSystemResource("/etc/arsnova/arsnova.properties"),
		});
		configurer.setIgnoreResourceNotFound(true);
		configurer.setIgnoreUnresolvablePlaceholders(false);
		return configurer;
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
	public ARSnovaSocketIOServer socketServer() {
		final ARSnovaSocketIOServer socketServer = new ARSnovaSocketIOServer();
		socketServer.setHostIp(socketIp);
		socketServer.setPortNumber(socketPort);
		socketServer.setUseSSL(socketUseSll);
		socketServer.setKeystore(socketKeystore);
		socketServer.setStorepass(socketStorepass);
		return socketServer;
	}

	@Profile("test")
	@Bean(name = "socketServer", initMethod = "startServer", destroyMethod = "stopServer")
	public ARSnovaSocketIOServer socketTestServer() {
		final int testSocketPort = 1234;
		final ARSnovaSocketIOServer socketServer = new ARSnovaSocketIOServer();
		socketServer.setHostIp(socketIp);
		socketServer.setPortNumber(socketPort + testSocketPort);
		socketServer.setUseSSL(socketUseSll);
		socketServer.setKeystore(socketKeystore);
		socketServer.setStorepass(socketStorepass);
		return socketServer;
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}
}
