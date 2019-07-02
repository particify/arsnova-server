package de.thm.arsnova.config;

import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.CollectionFactory;

public class PrefixedYamlPropertiesFactoryBean extends YamlPropertiesFactoryBean {
	private static final String PREFIX = "arsnova";

	@Override
	protected Properties createProperties() {
		final Properties result = CollectionFactory.createStringAdaptingProperties();
		process((properties, map) -> properties.forEach((k, v) -> {
			if (k.toString().startsWith(PREFIX + ".")) {
				result.put(k.toString().substring(PREFIX.length() + 1), v);
			}
		}));

		return result;
	}
}
