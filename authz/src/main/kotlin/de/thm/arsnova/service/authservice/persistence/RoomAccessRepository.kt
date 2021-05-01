package de.thm.arsnova.service.authservice.persistence

import de.thm.arsnova.service.authservice.model.RoomAccess
import de.thm.arsnova.service.authservice.model.RoomAccessPK
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date
import java.util.Optional

@Repository
interface RoomAccessRepository : CrudRepository<RoomAccess, RoomAccessPK> {
    fun findByRoomId(roomId: String): Iterable<RoomAccess>
    fun findByRoomIdAndRole(roomId: String, role: String): Iterable<RoomAccess>
    fun findByUserId(userId: String): Iterable<RoomAccess>
    @Query("""
        UPDATE room_access
            SET last_access = :lastAccess
            WHERE room_id = :roomId
            AND user_id = :userId
            RETURNING *;
        """,
        nativeQuery = true
    )
    fun updateLastAccessAndGetByRoomIdAndUserId(roomId: String, userId: String, lastAccess: Date): Optional<RoomAccess>
    // This is needed to not have hibernate check if any rows should be deleted
    @Query("DELETE FROM room_access WHERE room_id = :roomId RETURNING *;", nativeQuery = true)
    fun deleteByRoomIdWithoutChecking(@Param("roomId") roomId: String): Iterable<RoomAccess>
    // This is needed to not have hibernate check if any rows should be deleted
    @Query("DELETE FROM room_access WHERE room_id = :roomId and user_id = :userId RETURNING *;", nativeQuery = true)
    fun deleteByRoomIdAndUserIdWithoutChecking(
        @Param("roomId") roomId: String,
        @Param("userId") userId: String
    ): Iterable<RoomAccess>
    // This always sets the role to owner even if the entry was already present
    // The two role params are needed because otherwise Hibernate can't find the second usage
    @Query("""
        INSERT INTO room_access 
            (room_id, user_id, rev, role) 
            VALUES (:roomId, :userId, :rev, :role)
            ON CONFLICT ON CONSTRAINT room_access_pkey DO UPDATE SET role = :updateRole
            RETURNING *;
        """,
        nativeQuery = true
    )
    fun createOrUpdateAccess(
        @Param("roomId") roomId: String,
        @Param("userId") userId: String,
        @Param("rev") rev: String,
        @Param("role") role: String,
        @Param("updateRole") updateRole: String
    ): RoomAccess
    // This query should not be needed, but since the PK is composed, hibernate tries to update instead of inserting
    @Query("""
        INSERT INTO room_access
            (room_id, user_id, rev, role)
            VALUES (:roomId, :userId, :rev, :role)
            RETURNING *;
        """,
        nativeQuery = true
    )
    fun createAccess(
        @Param("roomId") roomId: String,
        @Param("userId") userId: String,
        @Param("rev") rev: String,
        @Param("role") role: String
    ): RoomAccess

    @Query(
        """
        SELECT COUNT(*)
        FROM room_access
        WHERE last_access > :lastAccess
        GROUP BY room_id;
        """,
        nativeQuery = true
    )
    fun countByLastAccessAfterAndGroupByRoomId(lastAccess: Date): List<Int>
}
