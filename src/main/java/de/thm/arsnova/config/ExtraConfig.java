package de.thm.arsnova.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class ExtraConfig {

	@Autowired
	private Environment env;

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
		if (!"true".equals(env.getProperty("connector.enable"))) {
			return null;
		}

		final ConnectorClientImpl connectorClient = new ConnectorClientImpl();
		connectorClient.setServiceLocation(env.getProperty("connector.uri"));
		connectorClient.setUsername(env.getProperty("connector.username"));
		connectorClient.setPassword(env.getProperty("connector.password"));
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
		final ARSnovaSocketIOServer socketServer = new ARSnovaSocketIOServer();
		socketServer.setHostIp(socketIp);
		socketServer.setPortNumber(socketPort + 1234);
		socketServer.setUseSSL(socketUseSll);
		socketServer.setKeystore(socketKeystore);
		socketServer.setStorepass(socketStorepass);
		return socketServer;
	}
}
