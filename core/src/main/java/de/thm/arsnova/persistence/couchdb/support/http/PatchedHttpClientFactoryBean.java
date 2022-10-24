package de.thm.arsnova.persistence.couchdb.support.http;

import org.ektorp.spring.HttpClientFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.DirectFieldAccessor;

public class PatchedHttpClientFactoryBean extends HttpClientFactoryBean {
  private static final Logger logger = LoggerFactory.getLogger(PatchedHttpClient.class);

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.couchDBProperties != null) {
      (new DirectFieldAccessor(this)).setPropertyValues(this.couchDBProperties);
    }

    logger.info("Starting couchDb connector on {}:{}...", new Object[]{this.host, this.port});
    logger.debug("host: {}", this.host);
    logger.debug("port: {}", this.port);
    logger.debug("url: {}", this.url);
    logger.debug("maxConnections: {}", this.maxConnections);
    logger.debug("connectionTimeout: {}", this.connectionTimeout);
    logger.debug("socketTimeout: {}", this.socketTimeout);
    logger.debug("autoUpdateViewOnChange: {}", this.autoUpdateViewOnChange);
    logger.debug("testConnectionAtStartup: {}", this.testConnectionAtStartup);
    logger.debug("cleanupIdleConnections: {}", this.cleanupIdleConnections);
    logger.debug("enableSSL: {}", this.enableSSL);
    logger.debug("relaxedSSLSettings: {}", this.relaxedSSLSettings);
    logger.debug("useExpectContinue: {}", this.useExpectContinue);
    logger.debug("Using non-caching implementation for HEAD requests.");
    this.client = (new PatchedHttpClient.Builder())
        .host(this.host)
        .port(this.port)
        .maxConnections(this.maxConnections)
        .connectionTimeout(this.connectionTimeout)
        .socketTimeout(this.socketTimeout)
        .username(this.username)
        .password(this.password)
        .cleanupIdleConnections(this.cleanupIdleConnections)
        .useExpectContinue(this.useExpectContinue)
        .enableSSL(this.enableSSL)
        .relaxedSSLSettings(this.relaxedSSLSettings)
        .sslSocketFactory(this.sslSocketFactory)
        .caching(this.caching)
        .maxCacheEntries(this.maxCacheEntries)
        .maxObjectSizeBytes(this.maxObjectSizeBytes)
        .url(this.url)
        .build();
    if (this.testConnectionAtStartup) {
      this.testConnect(this.client);
    }

    this.configureAutoUpdateViewOnChange();
  }
}
