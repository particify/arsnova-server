package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class RoomSummary (
    var id: String = "",
    var shortId: String = "",
    var name: String = "",
    var stats: RoomStats? = null
)

data class RoomStats (
    var contentCount: Int,
    var ackCommentCount: Int
)
