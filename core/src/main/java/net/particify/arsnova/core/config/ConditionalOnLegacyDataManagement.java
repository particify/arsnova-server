package net.particify.arsnova.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import net.particify.arsnova.core.config.properties.SystemProperties;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@ConditionalOnProperty(
    name = "external-data-management",
    prefix = SystemProperties.PREFIX,
    havingValue = "false",
    matchIfMissing = true)
public @interface ConditionalOnLegacyDataManagement {
}
