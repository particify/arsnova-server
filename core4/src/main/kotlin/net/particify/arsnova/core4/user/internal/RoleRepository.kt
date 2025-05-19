/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, UUID> {
  //  @Query(
  //      """
  //    SELECT id, name
  //    FROM user_role role
  //    JOIN user_role_mapping mapping
  //    ON mapping.role_id = role.id
  //    WHERE mapping.user_id = :userId
  //    """
  //  )
  @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
  fun findByUserId(userId: UUID): List<Role>
}
