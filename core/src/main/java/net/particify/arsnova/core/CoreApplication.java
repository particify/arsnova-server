package net.particify.arsnova.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.particify.arsnova.core.config.YamlPropertySourceFactory;

@SpringBootApplication
@EnableAsync(mode = AdviceMode.ASPECTJ)
@EnableCaching(mode = AdviceMode.ASPECTJ)
@EnableScheduling
@EnableSpringConfigured
@PropertySource(
    value = {
        "classpath:config/defaults.yml",
        "classpath:config/actuator.yml",
        "classpath:config/alias-words.yml",
        "file:${arsnova.config-dir:.}/application.yml",
        "file:${arsnova.config-dir:.}/secrets.yml",
        "file:${arsnova.config-dir:.}/ui.yml"},
    ignoreResourceNotFound = true,
    encoding = "UTF-8",
    factory = YamlPropertySourceFactory.class)
public class CoreApplication {
  public static void main(final String[] args) {
    SpringApplication.run(CoreApplication.class, args);
  }
}
