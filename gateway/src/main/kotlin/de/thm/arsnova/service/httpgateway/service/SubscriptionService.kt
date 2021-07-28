package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.model.FeatureSetting
import de.thm.arsnova.service.httpgateway.model.RoomFeatures
import de.thm.arsnova.service.httpgateway.model.RoomSubscription
import de.thm.arsnova.service.httpgateway.model.UserSubscription
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class SubscriptionService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties
) {
    companion object {
        const val FREE_TIER_STRING = "free"
        const val ROOM_FEATURE_STRING = "ROOM"
        const val ROOM_PARTICIPANT_LIMIT_KEY_STRING = "roomParticipantLimit"
        const val DEFAULT_ROOM_PARTICIPANT_LIMIT = 10000
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun getUserSubscription(userId: String, withTierId: Boolean): Mono<UserSubscription> {
        return if (httpGatewayProperties.httpClient.subscriptionService != null) {
            val url = "${httpGatewayProperties.httpClient.subscriptionService}/subscription/by-user?ids=$userId&withTierId=$withTierId"
            logger.trace("Querying subscription service for subscription by user with url: {}", url)
            webClient.get().uri(url)
                .retrieve().bodyToFlux(UserSubscription::class.java).next()
                .onErrorResume { error ->
                    logger.warn("Error from querying subscription service: {}", error)
                    Mono.just(UserSubscription(userId, FREE_TIER_STRING))
                }
        } else {
            Mono.just(UserSubscription(userId, FREE_TIER_STRING))
        }
    }

    fun getRoomSubscription(roomId: String, withTierId: Boolean): Mono<RoomSubscription> {
        return if (httpGatewayProperties.httpClient.subscriptionService != null) {
            val url = "${httpGatewayProperties.httpClient.subscriptionService}/subscription/by-room?ids=$roomId&withTierId=$withTierId"
            logger.trace("Querying subscription service for subscription by user with url: {}", url)
            webClient.get().uri(url)
                .retrieve().bodyToFlux(RoomSubscription::class.java).next()
                .onErrorResume { error ->
                    logger.warn("Error from querying subscription service: {}", error)
                    Mono.just(RoomSubscription(roomId, FREE_TIER_STRING))
                }
        } else {
            Mono.just(RoomSubscription(roomId, FREE_TIER_STRING))
        }
    }

    fun getRoomFeatures(roomId: String, withTierId: Boolean): Mono<RoomFeatures> {
        return if (httpGatewayProperties.httpClient.subscriptionService != null) {
            val url = "${httpGatewayProperties.httpClient.subscriptionService}/feature/by-room?ids=$roomId&withTierId=$withTierId"
            logger.trace("Querying subscription service for features by room with url: {}", url)
            webClient.get().uri(url)
                .retrieve().bodyToFlux(RoomFeatures::class.java).next()
                .onErrorResume { error ->
                    logger.warn("Error from querying subscription service: {}", error)
                    Mono.just(RoomFeatures(roomId, emptyList(), null))
                }
        } else {
            Mono.just(RoomFeatures(roomId, emptyList(), null))
        }
    }

    fun getRoomParticipantLimit(tierId: String): Mono<Int> {
        return if (httpGatewayProperties.httpClient.subscriptionService != null) {
            val url = "${httpGatewayProperties.httpClient.subscriptionService}/featuresettings/by-name-and-tierid-and-keys?name=$ROOM_FEATURE_STRING&tierId=$tierId&keys=$ROOM_PARTICIPANT_LIMIT_KEY_STRING"
            logger.trace("Querying subscription service for feature settingswith url: {}", url)
            webClient.get().uri(url)
                .retrieve().bodyToFlux(FeatureSetting::class.java).next()
                .switchIfEmpty(Mono.just(FeatureSetting(tierId, ROOM_FEATURE_STRING, DEFAULT_ROOM_PARTICIPANT_LIMIT.toString())))
                .map { featureSetting ->
                    featureSetting.value.toInt()
                }
                .onErrorResume { error ->
                    logger.warn("Error from querying subscription service: {}", error)
                    Mono.just(DEFAULT_ROOM_PARTICIPANT_LIMIT)
                }
        } else {
            Mono.just(DEFAULT_ROOM_PARTICIPANT_LIMIT)
        }
    }
}
