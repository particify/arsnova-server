/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnBooleanProperty(name = ["persistence.v3-migration.enabled"])
class MigrationExecutor(
    private val migrator: Migrator,
    private val applicationContext: ApplicationContext,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private var initEventHandled = false

  @EventListener(classes = [ContextRefreshedEvent::class])
  fun onApplicationEvent() {
    /* Event is triggered more than once */
    if (initEventHandled) {
      return
    }
    initEventHandled = true

    migrator.migrateUsers()
    migrator.migrateRooms()
    migrator.migrateAnnouncements()
    applicationEventPublisher.publishEvent(MigrationCompletedEvent())
    logger.info("Migration completed.")
    SpringApplication.exit(applicationContext)
  }
}
