package net.particify.arsnova.core.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {
  @Override
  public PropertySource<?> createPropertySource(final String name, final EncodedResource resource)
      throws IOException {
    try {
      final YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new PrefixedYamlPropertiesFactoryBean();
      yamlPropertiesFactoryBean.setResources(resource.getResource());
      yamlPropertiesFactoryBean.afterPropertiesSet();
      final Properties properties = yamlPropertiesFactoryBean.getObject();

      return new PropertiesPropertySource(name != null ? name : resource.getResource().getFilename(), properties);
    } catch (final IllegalStateException e) {
      if (e.getCause() instanceof FileNotFoundException) {
        throw (FileNotFoundException) e.getCause();
      }
      throw e;
    }
  }
}
