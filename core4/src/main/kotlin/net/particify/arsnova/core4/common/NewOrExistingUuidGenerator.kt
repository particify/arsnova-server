/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import java.util.EnumSet
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.EventType
import org.hibernate.generator.EventTypeSets
import org.hibernate.id.IdentifierGenerator
import org.hibernate.id.uuid.StandardRandomStrategy
import org.hibernate.id.uuid.UuidValueGenerator
import org.hibernate.type.descriptor.java.UUIDJavaType
import org.hibernate.type.descriptor.java.UUIDJavaType.PassThroughTransformer

class NewOrExistingUuidGenerator : IdentifierGenerator {
  private val generator: UuidValueGenerator = StandardRandomStrategy.INSTANCE
  private val valueTransformer: UUIDJavaType.ValueTransformer = PassThroughTransformer.INSTANCE

  override fun getEventTypes(): EnumSet<EventType> = EventTypeSets.INSERT_ONLY

  override fun allowAssignedIdentifiers() = true

  override fun generate(session: SharedSessionContractImplementor?, owner: Any?): Any {
    return valueTransformer.transform(generator.generateUuid(session))
  }

  override fun generate(
      session: SharedSessionContractImplementor?,
      owner: Any?,
      currentValue: Any?,
      eventType: EventType?
  ): Any {
    return currentValue ?: generate(session, owner)
  }
}
