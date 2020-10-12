package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreStats(
        val userProfile: CoreEntityStats,
        val room: CoreEntityStats,
        val content: CoreEntityStats,
        val answer: CoreEntityStats
)

data class CoreEntityStats(
        val totalCount: Int
)
