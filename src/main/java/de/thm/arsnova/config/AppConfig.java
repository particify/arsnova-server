/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
import com.fasterxml.jackson.databind.SerializationFeature;
import de.thm.arsnova.ImageUtils;
import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.client.ConnectorClientImpl;
import de.thm.arsnova.entities.serialization.CouchDbObjectMapperFactory;
import de.thm.arsnova.socket.ARSnovaSocket;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;
import de.thm.arsnova.web.CacheControlInterceptorHandler;
import de.thm.arsnova.web.CorsFilter;
import de.thm.arsnova.web.DeprecatedApiInterceptorHandler;
import de.thm.arsnova.web.ResponseInterceptorHandler;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.spring.HttpClientFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads property file and configures non-security related beans and components.
 *
 * expose-proxy for AspectJ is needed to access the proxy object via AopContext.currentProxy() in CouchDBDao. It might
 * have a negative impact on performance but is needed for caching until a better solution is implemented (e.g. use of
 * AspectJ's weaving).
 */
@ComponentScan({
		"de.thm.arsnova.aop",
		"de.thm.arsnova.cache",
		"de.thm.arsnova.controller",
		"de.thm.arsnova.domain",
		"de.thm.arsnova.dao",
		"de.thm.arsnova.events",
		"de.thm.arsnova.security",
		"de.thm.arsnova.services",
		"de.thm.arsnova.web"})
@Configuration
@EnableAsync
@EnableCaching(mode = AdviceMode.ASPECTJ)
@EnableScheduling
@EnableSpringConfigured
@EnableWebMvc
@PropertySource(
		value = {"classpath:arsnova.properties.example", "file:/etc/arsnova/arsnova.properties"},
		ignoreResourceNotFound = true
)
public class AppConfig extends WebMvcConfigurerAdapter {
	public static final MediaType API_V2_MEDIA_TYPE = new MediaType("application", "vnd.de.thm.arsnova.v2+json");
	public static final MediaType API_V3_MEDIA_TYPE = new MediaType("application", "vnd.de.thm.arsnova.v3+json");

	@Autowired
	private Environment env;

	@Value("${couchdb.name}") private String couchDbName;
	@Value("${couchdb.host}") private String couchDbHost;
	@Value("${couchdb.port}") private int couchDbPort;

	@Value(value = "${connector.enable}") private boolean connectorEnable;
	@Value(value = "${connector.uri}") private String connectorUri;
	@Value(value = "${connector.username}") private String connectorUsername;
	@Value(value = "${connector.password}") private String connectorPassword;

	@Value(value = "${socketio.bind-address}") private String socketAddress;
	@Value(value = "${socketio.port}") private int socketPort;
	@Value(value = "${socketio.ssl.jks-file:}") private String socketKeystore;
	@Value(value = "${socketio.ssl.jks-password:}") private String socketKeystorePassword;
	@Value(value = "${security.cors.origins:}") private String[] corsOrigins;
	@Value(value = "${mail.host}") private String mailHost;
	@Value(value = "${api.indent-response-body:false}") private boolean apiIndent;

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(stringMessageConverter());
		converters.add(defaultJsonMessageConverter());
		converters.add(apiV2JsonMessageConverter());
		//converters.add(new MappingJackson2XmlHttpMessageConverter(builder.createXmlMapper(true).build()));
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.mediaType("json", MediaType.APPLICATION_JSON_UTF8);
		configurer.mediaType("xml", MediaType.APPLICATION_XML);
		configurer.defaultContentType(API_V3_MEDIA_TYPE);
		configurer.favorParameter(true);
		configurer.favorPathExtension(false);
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		registry.viewResolver(new InternalResourceViewResolver());
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(cacheControlInterceptorHandler());
		registry.addInterceptor(deprecatedApiInterceptorHandler());
		registry.addInterceptor(responseInterceptorHandler());
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("swagger.json").addResourceLocations("classpath:/");
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
		StringHttpMessageConverter messageConverter = new StringHttpMessageConverter();
		List<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(MediaType.TEXT_PLAIN);
		messageConverter.setSupportedMediaTypes(mediaTypes);

		return messageConverter;
	}

	@Bean
	public MappingJackson2HttpMessageConverter defaultJsonMessageConverter() {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder
				.serializationInclusion(JsonInclude.Include.NON_EMPTY)
				.defaultViewInclusion(false)
				.indentOutput(apiIndent)
				.simpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(builder.build());
		List<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(API_V3_MEDIA_TYPE);
		mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	@Bean
	public MappingJackson2HttpMessageConverter apiV2JsonMessageConverter() {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder
				.serializationInclusion(JsonInclude.Include.NON_EMPTY)
				.defaultViewInclusion(false)
				.indentOutput(apiIndent)
				.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.featuresToEnable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(builder.build());
		List<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(API_V2_MEDIA_TYPE);
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
		PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean.setLocation(new ClassPathResource("version.properties"));

		return propertiesFactoryBean;
	}

	@Bean
	public CorsFilter corsFilter() {
		return new CorsFilter(Arrays.asList(corsOrigins));
	}

	@Bean
	public StdCouchDbConnector couchDbConnector() throws Exception {
		return new StdCouchDbConnector(couchDbName, couchDbInstance(), new CouchDbObjectMapperFactory());
	}

	@Bean
	public StdCouchDbInstance couchDbInstance() throws Exception {
		return new StdCouchDbInstance(couchDbHttpClientFactory().getObject());
	}

	@Bean
	public HttpClientFactoryBean couchDbHttpClientFactory() throws Exception {
		final HttpClientFactoryBean factory = new HttpClientFactoryBean();
		factory.setHost(couchDbHost);
		factory.setPort(couchDbPort);

		return factory;
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

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

	@Bean
	public JavaMailSenderImpl mailSender() {
		final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(mailHost);

		return mailSender;
	}

	@Bean
	public ImageUtils imageUtils() {
		return new ImageUtils();
	}
}
