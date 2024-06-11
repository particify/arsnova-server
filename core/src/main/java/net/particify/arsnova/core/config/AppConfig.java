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

package net.particify.arsnova.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import net.particify.arsnova.common.util.YamlPropertiesLoader;
import net.particify.arsnova.connector.client.ConnectorClient;
import net.particify.arsnova.connector.client.ConnectorClientImpl;
import net.particify.arsnova.core.config.properties.AliasWordsProperties;
import net.particify.arsnova.core.config.properties.PresetsProperties;
import net.particify.arsnova.core.config.properties.SecurityProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.config.properties.SystemProperties.Mail;
import net.particify.arsnova.core.config.properties.TemplateProperties;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.web.CustomCorsFilter;
import net.particify.arsnova.core.web.PathBasedContentNegotiationStrategy;

/**
 * Loads property file and configures non-security related beans and components.
 *
 * <p>
 * expose-proxy for AspectJ is needed to access the proxy object via AopContext.currentProxy() in CouchDBDao. It might
 * have a negative impact on performance but is needed for caching until a better solution is implemented (e.g. use of
 * AspectJ's weaving).
 * </p>
 */
@Configuration
@EnableConfigurationProperties({
    AliasWordsProperties.class,
    PresetsProperties.class,
    SystemProperties.class,
    TemplateProperties.class})
public class AppConfig implements WebMvcConfigurer {
  public static final String API_V3_MEDIA_TYPE_VALUE = "application/vnd.de.thm.arsnova.v3+json";
  public static final MediaType API_V3_MEDIA_TYPE = MediaType.valueOf(API_V3_MEDIA_TYPE_VALUE);
  public static final MediaType CSV_MEDIA_TYPE = new MediaType("text", "csv");
  public static final MediaType TSV_MEDIA_TYPE = new MediaType("text", "tab-separated-values");
  public static final String LMS_CONNECTOR_BEAN_NAME = "lmsConnectorClient";

  private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

  @Autowired
  private SystemProperties systemProperties;

  @Autowired
  private SecurityProperties securityProperties;

  @Autowired
  private WebEndpointProperties webEndpointProperties;

  @Override
  public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
    converters.add(defaultJsonMessageConverter());
    converters.add(stringMessageConverter());
    converters.add(byteArrayHttpMessageConverter());
  }

  @Override
  public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
    final List<MediaType> fallbacks = List.of(
        API_V3_MEDIA_TYPE,
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML);
    final PathBasedContentNegotiationStrategy strategy =
        new PathBasedContentNegotiationStrategy(fallbacks, webEndpointProperties.getBasePath());
    configurer.mediaType("json", MediaType.APPLICATION_JSON);
    configurer.mediaType("xml", MediaType.APPLICATION_XML);
    configurer.defaultContentTypeStrategy(strategy);
  }

  @Override
  public void configureViewResolvers(final ViewResolverRegistry registry) {
    registry.viewResolver(new InternalResourceViewResolver());
  }

  @Bean
  public StringHttpMessageConverter stringMessageConverter() {
    final StringHttpMessageConverter messageConverter = new StringHttpMessageConverter();
    messageConverter.setDefaultCharset(StandardCharsets.UTF_8);
    messageConverter.setWriteAcceptCharset(false);
    final List<MediaType> mediaTypes = new ArrayList<>();
    mediaTypes.add(MediaType.TEXT_PLAIN);
    mediaTypes.add(MediaType.APPLICATION_XML);
    messageConverter.setSupportedMediaTypes(mediaTypes);

    return messageConverter;
  }

  @Bean
  public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
    final ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
    arrayHttpMessageConverter.setSupportedMediaTypes(List.of(
        MediaType.APPLICATION_OCTET_STREAM,
        CSV_MEDIA_TYPE,
        TSV_MEDIA_TYPE
    ));

    return arrayHttpMessageConverter;
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
    mapper.setConfig(mapper.getSerializationConfig()
        .without(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
        .withView(View.Public.class));
    final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
    final List<MediaType> mediaTypes = new ArrayList<>();
    mediaTypes.add(API_V3_MEDIA_TYPE);
    mediaTypes.add(MediaType.APPLICATION_JSON);
    converter.setSupportedMediaTypes(mediaTypes);

    return converter;
  }

  @Bean
  public StandardServletMultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver();
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
  public CustomCorsFilter customCorsFilter() {
    return new CustomCorsFilter(securityProperties.getCorsOrigins());
  }

  @Bean(name = LMS_CONNECTOR_BEAN_NAME)
  @ConditionalOnProperty(
      name =  "enabled",
      prefix = SystemProperties.PREFIX + ".lms-connector"
  )
  public ConnectorClient connectorClient() {
    logger.info("LMS connector is enabled.");
    final ConnectorClientImpl connectorClient = new ConnectorClientImpl();
    connectorClient.setServiceLocation(systemProperties.getLmsConnector().getHostUrl());
    connectorClient.setUsername(systemProperties.getLmsConnector().getUsername());
    connectorClient.setPassword(systemProperties.getLmsConnector().getPassword());
    return connectorClient;
  }

  @Bean
  public CacheManager cacheManager(final Caffeine caffeine) {
    final CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.setCacheNames(List.of(
        "entity",
        "leaderboard-content",
        "leaderboard-contentgroup",
        "rendered-texts",
        "room.id-by-shortid",
        "system"));
    caffeineCacheManager.setCaffeine(caffeine);

    return caffeineCacheManager;
  }

  @Bean
  public Caffeine caffeineConfig() {
    final long expiryInMinutes = systemProperties.getCaching().getExpiry().toMinutes();
    return Caffeine.newBuilder()
        .expireAfterAccess(expiryInMinutes, TimeUnit.MINUTES)
        .maximumSize(systemProperties.getCaching().getMaxEntries())
        .recordStats();
  }

  @Bean
  public JavaMailSenderImpl mailSender() {
    final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    final Mail mailProperties = systemProperties.getMail();
    if (mailProperties.getImplicitTls()) {
      mailSender.getJavaMailProperties().setProperty("mail.transport.protocol", "smtps");
    }
    mailSender.setHost(mailProperties.getHost());
    if (mailProperties.getPort() > 0) {
      mailSender.setPort(mailProperties.getPort());
    }
    if (mailProperties.getUsername() != null && !mailProperties.getUsername().isEmpty()
        && mailProperties.getPassword() != null && !mailProperties.getPassword().isEmpty()) {
      mailSender.setUsername(mailProperties.getUsername());
      mailSender.setPassword(mailProperties.getPassword());
      mailSender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
      mailSender.getJavaMailProperties().setProperty("mail.smtps.auth", "true");
    }

    // Enable STARTTLS when the mail server has a public IP address and
    // implicit TLS is disabled. Internal mail servers usually use
    // self-signed certificates which cannot be verified without a trust
    // store. Implicit TLS can be used if encryption is required.
    boolean localAddress = false;
    try {
      final InetAddress address = InetAddress.getByName(mailProperties.getHost());
      localAddress = address.isLoopbackAddress() || address.isSiteLocalAddress();
      logger.debug("Mail server IP address: {} (is local address? {})",
          address.getHostAddress(), localAddress);
    } catch (final UnknownHostException e) {
      logger.error("Cannot resolve hostname for mail server.", e);
    }
    if (!localAddress && !mailProperties.getImplicitTls()) {
      logger.info("Enabling STARTTLS for mail server connections.");
      mailSender.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "true");
    }
    // When run inside a container, the FQDN of the host cannot be detected.
    // Therefore, it can be overridden manually.
    if (mailProperties.getLocalhost() != null) {
      mailSender.getJavaMailProperties().setProperty("mail.smtp.localhost", mailProperties.getLocalhost());
      mailSender.getJavaMailProperties().setProperty("mail.host", mailProperties.getLocalhost());
    }

    return mailSender;
  }

  @Bean
  public MessageSource yamlMessageSource() {
    final ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:messages/messages");
    messageSource.setPropertiesPersister(new YamlPropertiesLoader());
    messageSource.setFileExtensions(List.of(".yml", ".yaml"));
    return messageSource;
  }
}
