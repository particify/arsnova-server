/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.Size
import java.util.UUID
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.user.User

@Entity
@Table(schema = "room", name = "user_alias")
class RoomUserAlias(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne var room: Room? = null,
    @ManyToOne var user: User? = null,
    @field:Size(max = MAX_ALIAS_LENGTH) var alias: String? = null,
    /** Used to generate a localized alias if no [alias] is set. */
    var seed: Int? = null,
) {
  companion object {
    const val MAX_ALIAS_LENGTH = 30
  }
}
