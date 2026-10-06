/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@ConditionalOnBooleanProperty(
    name = ["persistence.v3-migration.enabled"], havingValue = false, matchIfMissing = true)
// Spring AOT drops the by-type autowiring that otherwise finds the AuditorAware bean.
@EnableJpaAuditing(auditorAwareRef = "entityAuditor")
class JpaAuditingConfiguration
