/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@ConditionalOnBooleanProperty(
    name = ["persistence.v3-migration.enabled"], havingValue = false, matchIfMissing = true)
@EnableJpaAuditing
class JpaAuditingConfiguration
