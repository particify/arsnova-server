package de.thm.arsnova.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.services.UserSessionService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Component
public class ARSnovaEventListener implements ApplicationListener<ARSnovaEvent> {

	public static final Logger LOGGER = LoggerFactory.getLogger(ARSnovaEventListener.class);

	@Autowired
	private ARSnovaSocketIOServer socketIoServer;

	@Autowired
	private UserSessionService userSessionService;

	@Override
	public void onApplicationEvent(ARSnovaEvent event) {
		userSessionService.sendEventViaWebSocket(socketIoServer, event);
	}
}
