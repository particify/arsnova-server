package de.thm.arsnova.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.client.ConnectorClientImpl;

@Configuration
@PropertySources({
	@PropertySource("arsnova.properties.example"),
	@PropertySource("file:///etc/arsnova/connector.properties"),
})
public class ExtraConfig {

	@Autowired
	private Environment env;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean(name = "connectorClient")
	public ConnectorClient connectorClient() {
		if (! "true".equals(env.getProperty("connector.enable"))) {
			return null;
		}

		ConnectorClientImpl connectorClient = new ConnectorClientImpl();
		connectorClient.setServiceLocation(env.getProperty("connector.uri"));
		connectorClient.setUsername(env.getProperty("connector.username"));
		connectorClient.setPassword(env.getProperty("connector.password"));
		return connectorClient;
	}
}
