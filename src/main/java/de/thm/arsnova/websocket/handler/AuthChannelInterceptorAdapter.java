package de.thm.arsnova.websocket.handler;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import de.thm.arsnova.security.User;
import de.thm.arsnova.service.UserService;

@Component
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(AuthChannelInterceptorAdapter.class);

	private final UserService service;

	@Autowired
	public AuthChannelInterceptorAdapter(final UserService service) {
		this.service = service;
	}

	@Nullable
	@Override
	public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
		logger.trace("Inspecting incoming message: {}", message);
		final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		final String sessionId = accessor.getSessionId();
		if (accessor.getCommand() != null && accessor.getCommand().equals(StompCommand.CONNECT)) {
			// user needs to authorize
			logger.trace("Incoming message is a connect command");
			final List<String> tokenList = accessor.getNativeHeader("token");
			if (tokenList != null && tokenList.size() > 0) {
				logger.trace("Adding token {} to the ws session mapping", tokenList.get(0));
				final String token = tokenList.get(0);
				service.addWsSessionToJwtMapping(sessionId, token);
			} else {
				// no token given -> auth failed
				logger.debug("no auth token given, dropping connection attempt");
				return null;
			}
		} else {
			logger.trace("Incoming message is anything but a connect command");
			final List<String> userIdList = accessor.getNativeHeader("ars-user-id");
			if (userIdList != null && userIdList.size() > 0) {
				// user-id is given, check for auth
				logger.trace("Checking user id with ws session mapping");
				final String userId = userIdList.get(0);
				final User u = service.getAuthenticatedUserByWsSession(sessionId);
				if (u == null || !userId.equals(u.getId())) {
					// user isn't authorized, drop message
					logger.debug("user-id not validated, dropping frame");
					return null;
				}
			}
		}

		// default is to pass the frame along
		return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
	}
}
