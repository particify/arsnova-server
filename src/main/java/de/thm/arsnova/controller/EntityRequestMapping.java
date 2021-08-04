package de.thm.arsnova.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.thm.arsnova.config.AppConfig;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(produces = {
		AppConfig.API_V3_MEDIA_TYPE_VALUE,
		MediaType.APPLICATION_JSON_VALUE
})
public @interface EntityRequestMapping {
	@AliasFor(annotation = RequestMapping.class, attribute = "value")
	String[] value() default {};

	@AliasFor(annotation = RequestMapping.class, attribute = "method")
	RequestMethod[] method() default {};

	@AliasFor(annotation = RequestMapping.class, attribute = "params")
	String[] params() default {};

	@AliasFor(annotation = RequestMapping.class, attribute = "headers")
	String[] headers() default {};

	@AliasFor(annotation = RequestMapping.class, attribute = "consumes")
	String[] consumes() default {};
}
