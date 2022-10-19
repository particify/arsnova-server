package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class CommentStats(
    var roomId: String = "",
    var ackCommentCount: Int?
)
