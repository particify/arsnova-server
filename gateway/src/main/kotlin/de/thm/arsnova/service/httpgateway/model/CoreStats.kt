package de.thm.arsnova.service.httpgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreStats(
        val userProfile: CoreUserProfileStats,
        val room: CoreEntityStats,
        val content: CoreEntityStats,
        val answer: CoreEntityStats
)

data class CoreEntityStats(
        val totalCount: Int
)

data class CoreUserProfileStats(
        val accountCount: Int,
        val activationsPending: Int
)
