package de.thm.arsnova.service.wsgateway.config

import de.thm.arsnova.service.wsgateway.adapter.AuthChannelInterceptorAdapter
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableConfigurationProperties(WebSocketProperties::class)
@EnableWebSocketMessageBroker
class WebSocketConfig(
		private val webSocketProperties: WebSocketProperties,
		private val authChannelInterceptorAdapter: AuthChannelInterceptorAdapter
) : WebSocketMessageBrokerConfigurer {
	private val logger = LoggerFactory.getLogger(javaClass)


	override fun configureMessageBroker(config: MessageBrokerRegistry) {
		config
			.setApplicationDestinationPrefixes(webSocketProperties.messagingPrefix)
			.enableStompBrokerRelay(*webSocketProperties.stomp.destinationPrefix)
			.setUserRegistryBroadcast(webSocketProperties.stomp.userRegistryBroadcast)
			.setUserDestinationBroadcast(webSocketProperties.stomp.userDestinationBroadcast)
			.setRelayHost(webSocketProperties.stomp.relay.host)
			.setRelayPort(webSocketProperties.stomp.relay.port)
			.setClientLogin(webSocketProperties.stomp.relay.user)
			.setClientPasscode(webSocketProperties.stomp.relay.password)
	}

	override fun registerStompEndpoints(registry: StompEndpointRegistry) {
		registry.addEndpoint("/ws").withSockJS()
	}


	override fun configureClientInboundChannel(registration: ChannelRegistration) {
		registration.interceptors(authChannelInterceptorAdapter)
	}
}
