/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.license

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Licences are seeded from a CSV rather than managed through the API. A retired licence stays in
 * place for what was published under it and is only no longer [selectable]. [position] orders them
 * for selection, from the most to the least permissive as v3 listed them.
 */
@Entity
@Table(schema = "license")
class License(
    @Id var id: String? = null,
    var name: String? = null,
    var url: String? = null,
    var selectable: Boolean = true,
    var position: Int = 0,
)
