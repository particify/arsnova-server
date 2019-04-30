package de.thm.arsnova.config;

import de.thm.arsnova.websocket.handler.AuthChannelInterceptorAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final AuthChannelInterceptorAdapter authChannelInterceptorAdapter;
	@Value(value = "${security.cors.origins:}") private String[] corsOrigins;

	@Autowired
	public WebSocketConfig(AuthChannelInterceptorAdapter authChannelInterceptorAdapter) {
		this.authChannelInterceptorAdapter = authChannelInterceptorAdapter;
	}



	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config
				.setApplicationDestinationPrefixes("/backend")
				.enableSimpleBroker("/exchange", "/topic", "/queue");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(corsOrigins).withSockJS();
	}


	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.setInterceptors(authChannelInterceptorAdapter);
	}

}
