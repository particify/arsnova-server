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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.CharEncoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.client.ConnectorClientImpl;
import de.thm.arsnova.model.migration.FromV2Migrator;
import de.thm.arsnova.model.migration.ToV2Migrator;
import de.thm.arsnova.model.serialization.CouchDbDocumentModule;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.util.ImageUtils;
import de.thm.arsnova.web.CacheControlInterceptorHandler;
import de.thm.arsnova.web.CorsFilter;
import de.thm.arsnova.web.DeprecatedApiInterceptorHandler;
import de.thm.arsnova.web.PathBasedContentNegotiationStrategy;
import de.thm.arsnova.web.ResponseInterceptorHandler;
import de.thm.arsnova.websocket.ArsnovaSocketioServer;
import de.thm.arsnova.websocket.ArsnovaSocketioServerImpl;

/**
 * Loads property file and configures non-security related beans and components.
 *
 * <p>
 * expose-proxy for AspectJ is needed to access the proxy object via AopContext.currentProxy() in CouchDBDao. It might
 * have a negative impact on performance but is needed for caching until a better solution is implemented (e.g. use of
 * AspectJ's weaving).
 * </p>
 */
@ComponentScan({
		"de.thm.arsnova.cache",
		"de.thm.arsnova.controller",
		"de.thm.arsnova.event",
		"de.thm.arsnova.management",
		"de.thm.arsnova.security",
		"de.thm.arsnova.service",
		"de.thm.arsnova.web",
		"de.thm.arsnova.websocket.handler"})
@Configuration
@EnableAsync(mode = AdviceMode.ASPECTJ)
@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class})
@EnableCaching(mode = AdviceMode.ASPECTJ)
@EnableScheduling
@EnableSpringConfigured
@EnableWebMvc
@PropertySource(
		value = {
			"classpath:config/defaults.yml",
			"classpath:config/actuator.yml",
			"file:${arsnova.config-dir:.}/application.yml"},
		ignoreResourceNotFound = true,
		encoding = CharEncoding.UTF_8,
		factory = YamlPropertySourceFactory.class
)
@EnableConfigurationProperties(SystemProperties.class)
public class AppConfig implements WebMvcConfigurer {
	public static final String API_V2_MEDIA_TYPE_VALUE = "application/vnd.de.thm.arsnova.v2+json";
	public static final String API_V3_MEDIA_TYPE_VALUE = "application/vnd.de.thm.arsnova.v3+json";
	public static final MediaType API_V2_MEDIA_TYPE = MediaType.valueOf(API_V2_MEDIA_TYPE_VALUE);
	public static final MediaType API_V3_MEDIA_TYPE = MediaType.valueOf(API_V3_MEDIA_TYPE_VALUE);
	public static final MediaType ACTUATOR_MEDIA_TYPE = MediaType.valueOf(ActuatorMediaType.V2_JSON);

	@Autowired
	private Environment env;

	@Autowired
	private SystemProperties systemProperties;

	@Autowired
	private SecurityProperties securityProperties;

	@Autowired
	private WebEndpointProperties webEndpointProperties;

	@Override
	public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
		converters.add(defaultJsonMessageConverter());
		converters.add(apiV2JsonMessageConverter());
		converters.add(managementJsonMessageConverter());
		converters.add(stringMessageConverter());
		//converters.add(new MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build()));
	}

	@Override
	public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
		final PathBasedContentNegotiationStrategy strategy =
				new PathBasedContentNegotiationStrategy(API_V3_MEDIA_TYPE, webEndpointProperties.getBasePath());
		configurer.mediaType("json", MediaType.APPLICATION_JSON_UTF8);
		configurer.mediaType("xml", MediaType.APPLICATION_XML);
		configurer.favorParameter(false);
		configurer.favorPathExtension(false);
		//configurer.defaultContentType(API_V3_MEDIA_TYPE);
		configurer.defaultContentTypeStrategy(strategy);
	}

	@Override
	public void configurePathMatch(final PathMatchConfigurer configurer) {
		configurer.setUseSuffixPatternMatch(false);
	}

	@Override
	public void configureViewResolvers(final ViewResolverRegistry registry) {
		registry.viewResolver(new InternalResourceViewResolver());
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(cacheControlInterceptorHandler());
		registry.addInterceptor(deprecatedApiInterceptorHandler());
		registry.addInterceptor(responseInterceptorHandler());
	}

	@Override
	public void addResourceHandlers(final ResourceHandlerRegistry registry) {
		registry.addResourceHandler("swagger.json").addResourceLocations("classpath:/");
	}

	/* Provides a Spring Framework (non-Boot) compatible Filter. */
	@Bean
	public WebMvcMetricsFilter webMvcMetricsFilterOverride(
			final MeterRegistry registry, final WebMvcTagsProvider tagsProvider) {
		final MetricsProperties.Web.Server serverProperties = new MetricsProperties.Web.Server();
		return new WebMvcMetricsFilter(registry, tagsProvider,
				serverProperties.getRequestsMetricName(), serverProperties.isAutoTimeRequests());
	}

	@Bean
	public CacheControlInterceptorHandler cacheControlInterceptorHandler() {
		return new CacheControlInterceptorHandler();
	}

	@Bean
	public DeprecatedApiInterceptorHandler deprecatedApiInterceptorHandler() {
		return new DeprecatedApiInterceptorHandler();
	}

	@Bean
	public ResponseInterceptorHandler responseInterceptorHandler() {
		return new ResponseInterceptorHandler();
	}

	@Bean
	public StringHttpMessageConverter stringMessageConverter() {
		final StringHttpMessageConverter messageConverter = new StringHttpMessageConverter();
		messageConverter.setDefaultCharset(StandardCharsets.UTF_8);
		messageConverter.setWriteAcceptCharset(false);
		final List<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(MediaType.TEXT_PLAIN);
		messageConverter.setSupportedMediaTypes(mediaTypes);

		return messageConverter;
	}

	@Bean
	public MappingJackson2HttpMessageConverter defaultJsonMessageConverter() {
		final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder
				.serializationInclusion(JsonInclude.Include.NON_EMPTY)
				.defaultViewInclusion(false)
				.indentOutput(systemProperties.getApi().isIndentResponseBody())
				.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		final ObjectMapper mapper = builder.build();
		mapper.setConfig(mapper.getSerializationConfig().withView(View.Public.class));
		final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
		final List<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(API_V3_MEDIA_TYPE);
		mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	@Bean
	public MappingJackson2HttpMessageConverter apiV2JsonMessageConverter() {
		final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder
				.serializationInclusion(JsonInclude.Include.NON_NULL)
				.defaultViewInclusion(false)
				.indentOutput(systemProperties.getApi().isIndentResponseBody())
				.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.featuresToEnable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
				.modules(new CouchDbDocumentModule());
		final ObjectMapper mapper = builder.build();
		mapper.setConfig(mapper.getSerializationConfig().withView(View.Public.class));
		final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
		final List<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(API_V2_MEDIA_TYPE);
		mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	@Bean
	public MappingJackson2HttpMessageConverter managementJsonMessageConverter() {
		final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder
				.indentOutput(systemProperties.getApi().isIndentResponseBody())
				.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		final ObjectMapper mapper = builder.build();
		final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
		final List<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(ACTUATOR_MEDIA_TYPE);
		mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setIgnoreUnresolvablePlaceholders(false);

		return configurer;
	}

	@Bean
	public PropertiesFactoryBean versionInfoProperties() {
		final PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean.setLocation(new ClassPathResource("version.properties"));

		return propertiesFactoryBean;
	}

	@Bean
	public CorsFilter corsFilter() {
		return new CorsFilter(securityProperties.getCorsOrigins());
	}

	@Bean(name = "connectorClient")
	public ConnectorClient connectorClient() {
		if (!systemProperties.getLmsConnector().isEnabled()) {
			return null;
		}

		final ConnectorClientImpl connectorClient = new ConnectorClientImpl();
		connectorClient.setServiceLocation(systemProperties.getLmsConnector().getHostUrl());
		connectorClient.setUsername(systemProperties.getLmsConnector().getUsername());
		connectorClient.setPassword(systemProperties.getLmsConnector().getPassword());
		return connectorClient;
	}

	@Profile("!test")
	@Bean(name = "socketServer", initMethod = "startServer", destroyMethod = "stopServer")
	public ArsnovaSocketioServer socketServer() {
		final ArsnovaSocketioServerImpl socketioServer = new ArsnovaSocketioServerImpl();
		socketioServer.setHostIp(systemProperties.getSocketio().getBindAddress());
		socketioServer.setPortNumber(systemProperties.getSocketio().getPort());

		return socketioServer;
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

	@Bean
	public JavaMailSenderImpl mailSender() {
		final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(systemProperties.getMail().getHost());

		return mailSender;
	}

	@Bean
	public ImageUtils imageUtils() {
		return new ImageUtils();
	}

	@Bean
	public FromV2Migrator fromV2Migrator() {
		return new FromV2Migrator();
	}

	@Bean
	public ToV2Migrator toV2Migrator() {
		return new ToV2Migrator();
	}
}
