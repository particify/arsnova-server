/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

private const val CONTEXT_PATH = "/metadata"
private const val STATUS_OK = 200
private const val STATUS_NOT_MODIFIED = 304
private const val NO_BODY = -1L

private val HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

/** Which cache validators the server offers with a metadata document. */
enum class Saml2MetadataValidators {
  /** None at all, which is what Keycloak's descriptor endpoint does. */
  NONE,
  ETAG,
  LAST_MODIFIED
}

/**
 * Serves a mutable metadata document over HTTP, so the refresh path can be driven without an
 * identity provider to talk to. It binds an ephemeral port on construction and has to be started
 * before the Spring context whose properties point at it.
 */
class Saml2TestMetadataServer(private var document: String) {
  private val server: HttpServer = HttpServer.create(InetSocketAddress(0), 0)
  private val requests = AtomicInteger()
  private var entityTag = newEntityTag()
  private var lastModified: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)

  var validators = Saml2MetadataValidators.NONE
  var status = STATUS_OK

  val url: String
    get() = "http://localhost:${server.address.port}$CONTEXT_PATH"

  /** Every request the server answered, a conditional one which got a 304 included. */
  val requestCount: Int
    get() = requests.get()

  init {
    server.createContext(CONTEXT_PATH, this::handle)
    server.start()
  }

  fun stop() {
    server.stop(0)
  }

  /** Publishes a new document, which invalidates whichever validator the server hands out. */
  fun publish(document: String) {
    this.document = document
    entityTag = newEntityTag()
    lastModified = Instant.now().truncatedTo(ChronoUnit.SECONDS)
  }

  private fun handle(exchange: HttpExchange) {
    requests.incrementAndGet()
    try {
      when {
        status != STATUS_OK -> respond(exchange, status, ByteArray(0))
        isUnchanged(exchange) -> respond(exchange, STATUS_NOT_MODIFIED, ByteArray(0))
        else -> respondWithDocument(exchange)
      }
    } finally {
      exchange.close()
    }
  }

  private fun respondWithDocument(exchange: HttpExchange) {
    exchange.responseHeaders.add("Content-Type", "application/samlmetadata+xml")
    when (validators) {
      Saml2MetadataValidators.ETAG -> exchange.responseHeaders.add("ETag", entityTag)
      Saml2MetadataValidators.LAST_MODIFIED ->
          exchange.responseHeaders.add("Last-Modified", HTTP_DATE.format(lastModified))
      Saml2MetadataValidators.NONE -> Unit
    }
    respond(exchange, STATUS_OK, document.toByteArray())
  }

  private fun isUnchanged(exchange: HttpExchange): Boolean =
      when (validators) {
        Saml2MetadataValidators.ETAG ->
            exchange.requestHeaders.getFirst("If-None-Match") == entityTag
        Saml2MetadataValidators.LAST_MODIFIED ->
            exchange.requestHeaders.getFirst("If-Modified-Since") == HTTP_DATE.format(lastModified)
        Saml2MetadataValidators.NONE -> false
      }

  private fun respond(exchange: HttpExchange, status: Int, body: ByteArray) {
    if (body.isEmpty()) {
      exchange.sendResponseHeaders(status, NO_BODY)
      return
    }
    exchange.sendResponseHeaders(status, body.size.toLong())
    exchange.responseBody.write(body)
  }

  private fun newEntityTag() = "\"${Instant.now().toEpochMilli()}-${requests.get()}\""
}
