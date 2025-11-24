/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(schema = "`user`")
class Role {
  @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null
  @Version var version: Int? = 0
  var name: String? = null
  @ManyToMany
  @JoinTable(
      schema = "user",
      name = "user_role_mapping",
      joinColumns = [JoinColumn(name = "role_id")],
      inverseJoinColumns = [JoinColumn(name = "user_id")],
  )
  var users: MutableSet<User> = mutableSetOf()
}
