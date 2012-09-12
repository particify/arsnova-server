package de.thm.arsnova.socket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.socket.message.Feedback;

public class ARSnovaSocketIOServer {

	@Autowired
	private ISessionService sessionService;	
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Map<String, User> session2user = new ConcurrentHashMap<String, User>();
	
	private int portNumber;
	private final Configuration config;
	private SocketIOServer server;


	public ARSnovaSocketIOServer() {
		config = new Configuration();

	}

	public void startServer() throws Exception {
		/**
		 * hack: listen to ipv4 adresses
		 */
		System.setProperty("java.net.preferIPv4Stack" , "true");
		
		config.setPort(portNumber);
		config.setHostname("0.0.0.0");
		server = new SocketIOServer(config);

		server.addEventListener("setFeedback", Feedback.class,
				new DataListener<Feedback>() {
					@Override
					public void onData(SocketIOClient client, Feedback data) {
						logger.info("setFeedback.onData: Client: {}, message: {}", new Object[] {client, data});
						User u = session2user.get(client.getSessionId().toString());
						if(u == null || sessionService.isUserInSession(u, data.getSessionkey()) == false) {
							return;
						}
						sessionService.postFeedback(data.getSessionkey(), data.getValue(), u);
						de.thm.arsnova.entities.Feedback fb = sessionService.getFeedback(data.getSessionkey());
						logger.info("fb: {}", fb);
						server.getBroadcastOperations().sendEvent("updateFeedback", fb.getValues());
					}
		});

		
		
		
		server.addConnectListener(new ConnectListener() {
	        @Override
	        public void onConnect(SocketIOClient client) {
	        	logger.info("addConnectListener.onConnect: Client: {}", new Object[] {client});
	        }
	    });
		
		server.addDisconnectListener(new DisconnectListener() {
	        @Override
	        public void onDisconnect(SocketIOClient client) {
	        	logger.info("addDisconnectListener.onDisconnect: Client: {}", new Object[] {client});
	        }
	    });
		
		server.start();
	}

	public void stopServer() throws Exception {
		logger.debug("In stopServer method of class: {}", getClass().getName());
		for(SocketIOClient client : server.getAllClients()) {
			client.disconnect();
		}
		server.stop();		

	}

	public int getPortNumber() {
		return portNumber;
	}

	@Required
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public boolean authorize(String session, User user) {
		return session2user.put(session, user) != null;		
	}
}