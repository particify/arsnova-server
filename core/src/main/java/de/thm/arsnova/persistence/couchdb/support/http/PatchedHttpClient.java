package de.thm.arsnova.persistence.couchdb.support.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.StdHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchedHttpClient extends StdHttpClient {
  private static final Logger logger = LoggerFactory.getLogger(PatchedHttpClient.class);

  public PatchedHttpClient(final HttpClient hc, final HttpClient backend) {
    super(hc, backend);
  }

  @Override
  public HttpResponse head(final String uri) {
    logger.debug("Using non-caching implementation for HEAD request.");
    return this.executeRequest(new HttpHead(uri), true);
  }

  public static class Builder extends StdHttpClient.Builder {
    public org.ektorp.http.HttpClient build() {
      final org.apache.http.client.HttpClient client = this.configureClient();
      org.apache.http.client.HttpClient cachingHttpClient = client;
      if (this.caching) {
        cachingHttpClient = WithCachingBuilder.withCaching(client, this.maxCacheEntries, this.maxObjectSizeBytes);
      }

      return new PatchedHttpClient(cachingHttpClient, client);
    }
  }
}
