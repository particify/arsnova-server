package de.thm.arsnova.service.httpgateway.model

data class SummarizedStats(
    val connectedUsers: Int,
    val users: Int,
    val rooms: Int,
    val contents: Int,
    val answers: Int,
    val comments: Int
)
