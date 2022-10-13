package de.thm.arsnova.service.httpgateway.model

import java.util.Date

data class Announcement(
  val id: String,
  val creationTimestamp: Date,
  val updateTimestamp: Date?,
  val roomId: String,
  val roomName: String?,
  val creatorId: String,
  val title: String,
  val body: String,
  val renderedBody: String
)
