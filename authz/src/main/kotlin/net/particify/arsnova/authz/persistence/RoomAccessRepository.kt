package net.particify.arsnova.authz.persistence

import net.particify.arsnova.authz.model.RoomAccess
import net.particify.arsnova.authz.model.RoomAccessPK
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional
import java.util.UUID

@Repository
interface RoomAccessRepository : CrudRepository<RoomAccess, RoomAccessPK> {
  fun findByRoomId(roomId: UUID): Iterable<RoomAccess>

  fun findByRoomIdAndRole(
    roomId: UUID,
    role: String,
  ): Iterable<RoomAccess>

  fun findByRoomIdAndRoleNot(
    roomId: UUID,
    role: String,
  ): Iterable<RoomAccess>

  fun findByUserId(userId: UUID): Iterable<RoomAccess>

  @Query(
    """
    SELECT ra.userId
    FROM RoomAccess ra
    GROUP BY ra.userId
    HAVING MIN(ra.lastAccess) < :lastAccessBefore
    """,
  )
  fun findUserIdsByLastAccessBefore(lastAccessBefore: Date): Iterable<String>

  @Query(
    """
    UPDATE room_access
      SET last_access = :lastAccess
      WHERE room_id = :roomId
      AND user_id = :userId
      RETURNING *;
    """,
    nativeQuery = true,
  )
  fun updateLastAccessAndGetByRoomIdAndUserId(
    roomId: UUID,
    userId: UUID,
    lastAccess: Date,
  ): Optional<RoomAccess>

  // This is needed to not have hibernate check if any rows should be deleted
  @Query("DELETE FROM room_access WHERE room_id = :roomId RETURNING *;", nativeQuery = true)
  fun deleteByRoomIdWithoutChecking(
    @Param("roomId") roomId: UUID,
  ): Iterable<RoomAccess>

  // This is needed to not have hibernate check if any rows should be deleted
  @Query("DELETE FROM room_access WHERE room_id = :roomId and user_id = :userId RETURNING *;", nativeQuery = true)
  fun deleteByRoomIdAndUserIdWithoutChecking(
    @Param("roomId") roomId: UUID,
    @Param("userId") userId: UUID,
  ): Iterable<RoomAccess>

  // This sets the role to owner even if the entry was already present
  // It also checks to not override a creator role that's maybe present
  // The two role params are needed because otherwise Hibernate can't find the second usage
  @Query(
    """
    INSERT INTO room_access
      (room_id, user_id, rev, role)
      VALUES (:roomId, :userId, :rev, :role)
      ON CONFLICT ON CONSTRAINT room_access_pkey DO UPDATE SET role = :updateRole WHERE room_access.role != 'OWNER'
      RETURNING *;
    """,
    nativeQuery = true,
  )
  fun createOrUpdateAccess(
    @Param("roomId") roomId: UUID,
    @Param("userId") userId: UUID,
    @Param("rev") rev: String,
    @Param("role") role: String,
    @Param("updateRole") updateRole: String,
  ): RoomAccess

  // This query should not be needed, but since the PK is composed, hibernate tries to update instead of inserting
  @Query(
    """
    INSERT INTO room_access
      (room_id, user_id, rev, role)
      VALUES (:roomId, :userId, :rev, :role)
      RETURNING *;
    """,
    nativeQuery = true,
  )
  fun createAccess(
    @Param("roomId") roomId: UUID,
    @Param("userId") userId: UUID,
    @Param("rev") rev: String,
    @Param("role") role: String,
  ): RoomAccess

  // This query is for creating participants and checks for duplicate key,
  // does a fake update to prevent exceptions but still returns the row
  @Query(
    """
    INSERT INTO room_access
      (room_id, user_id, rev, role)
      VALUES (:roomId, :userId, :rev, 'PARTICIPANT')
      ON CONFLICT (room_id, user_id) DO UPDATE SET room_id = :roomId
      RETURNING *;
    """,
    nativeQuery = true,
  )
  fun createParticipantAccess(
    @Param("roomId") roomId: UUID,
    @Param("userId") userId: UUID,
    @Param("rev") rev: String,
  ): RoomAccess

  @Query(
    """
    SELECT COUNT(*)
    FROM room_access
    WHERE last_access > :lastAccess
    GROUP BY room_id;
    """,
    nativeQuery = true,
  )
  fun countByLastAccessAfterAndGroupByRoomId(lastAccess: Date): List<Int>

  fun countByRoomIdAndRole(
    roomId: UUID,
    role: String,
  ): Long

  fun countDistinctUserIdByLastAccessAfter(lastAccess: Date): Long

  fun countDistinctUserIdByRoleAndLastAccessAfter(
    role: String,
    lastAccess: Date,
  ): Long
}
