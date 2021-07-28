package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class Stats(
    var wsGatewayStats: WsGatewayStats,
    var coreServiceStats: Map<String, Any>,
    var commentServiceStats: CommentServiceStats
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WsGatewayStats(
    var webSocketUserCount: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentServiceStats(
    var commentCount: Long,
    var voteCount: Long
)
