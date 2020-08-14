package de.thm.arsnova.service.wsgateway.service

import de.thm.arsnova.service.wsgateway.config.WebSocketProperties
import de.thm.arsnova.service.wsgateway.model.RoomAccess
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class RoomAccessService(
		private val webSocketProperties: WebSocketProperties,
		private val restTemplate: RestTemplate
) {
	companion object {
		val roomAccessString = "roomaccess"
	}

	private val logger = LoggerFactory.getLogger(RoomAccessService::class.java)

	private var roomAccessGetEndpoint = "${webSocketProperties.httpClient.authService}/$roomAccessString"

	fun getRoomAccess(roomId: String, userId: String): RoomAccess? {
		val url = "$roomAccessGetEndpoint/$roomId/$userId"
		logger.trace("Querying auth service for room access with url: {}", url)
		return restTemplate.getForObject(url, RoomAccess::class.java)
	}
}
