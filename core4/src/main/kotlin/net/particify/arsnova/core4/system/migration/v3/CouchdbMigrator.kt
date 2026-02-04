/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.collections.chunked
import kotlin.collections.forEach
import net.particify.arsnova.core4.system.migration.v3.MigrationHelper.typeReference
import org.hibernate.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

const val COUCHDB_RESULT_LIMIT = 200
const val COUCHDB_PROGRESS_INTERVAL = COUCHDB_RESULT_LIMIT * 5

@Component
class CouchdbMigrator(
    @PersistenceContext val entityManager: EntityManager,
    private val properties: MigrationProperties,
) {
  val logger: Logger = LoggerFactory.getLogger(this::class.java)

  val couchdbClient: RestClient =
      RestClient.builder()
          .baseUrl(properties.couchdb.url)
          .defaultHeaders {
            it.setBasicAuth(properties.couchdb.username, properties.couchdb.password)
            it.accept = listOf(MediaType.APPLICATION_JSON)
            it.contentType = MediaType.APPLICATION_JSON
          }
          .build()

  final inline fun <reified T : Entity> migrate(fn: (doc: T) -> Any?) {
    val design = T::class.simpleName
    var startKey = "\"\""
    var count = 0
    val migrationStart = Instant.now()
    entityManager.unwrap<Session>(Session::class.java).jdbcBatchSize = JDBC_BATCH_SIZE
    do {
      logger.trace(
          "Loading CouchDB data for migration (design: {}, startkey: {})...", design, startKey)
      val queryStart = Instant.now()
      val result =
          try {
            couchdbClient
                .get()
                .uri {
                  it.path("/_design/$design/_view/by_id")
                      .queryParam("reduce", false)
                      .queryParam("include_docs", true)
                      .queryParam("startkey", startKey)
                      .queryParam("limit", COUCHDB_RESULT_LIMIT + 1)
                      .build()
                }
                .retrieve()
                .body(typeReference<CouchdbResponse<T>>())!!
          } catch (e: RestClientException) {
            logger.error("Failed to parse {} document (startkey: {})", design, startKey)
            throw e
          }
      logger.debug("Query took {} ms.", queryStart.until(Instant.now(), ChronoUnit.MILLIS))
      result.rows.take(COUCHDB_RESULT_LIMIT).chunked(JDBC_BATCH_SIZE).forEach { chunk ->
        chunk.forEach {
          logger.trace("Migrating {} {}...", design, it.doc.id)
          val newEntity = fn(it.doc)
          if (newEntity != null) {
            entityManager.persist(newEntity)
          }
        }
        val flushStart = Instant.now()
        entityManager.flush()
        entityManager.clear()
        logger.debug("Flush took {} ms.", flushStart.until(Instant.now(), ChronoUnit.MILLIS))
      }
      count += result.rows.size.coerceAtMost(COUCHDB_RESULT_LIMIT)
      if (count % COUCHDB_PROGRESS_INTERVAL == 0) {
        logger.info("Migration progress: {} {} documents migrated.", count, design)
      }
      if (result.rows.size > COUCHDB_RESULT_LIMIT)
          startKey = "\"" + result.rows[COUCHDB_RESULT_LIMIT].doc.id + "\""
    } while (result.rows.size > COUCHDB_RESULT_LIMIT)
    logger.info(
        "Migration of {} {} documents completed in {} seconds.",
        count,
        design,
        migrationStart.until(Instant.now(), ChronoUnit.SECONDS))
  }
}
