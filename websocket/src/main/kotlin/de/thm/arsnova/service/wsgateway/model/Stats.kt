package de.thm.arsnova.service.wsgateway.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class Stats (
  val webSocketUserCount: Int
)
