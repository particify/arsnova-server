package de.thm.arsnova.websockets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;

public class Message extends WebSocketServlet {

	private static final long serialVersionUID = 1L;
	private volatile int byteBufSize;
	private volatile int charBufSize;

	@Override
	public void init() throws ServletException {
		System.out.println("init();");
		super.init();
		byteBufSize = getInitParameterIntValue("byteBufferMaxSize", 2097152);
		charBufSize = getInitParameterIntValue("charBufferMaxSize", 2097152);
	}

	public int getInitParameterIntValue(String name, int defaultValue) {
		String val = this.getInitParameter(name);
		int result;
		if (null != val) {
			try {
				result = Integer.parseInt(val);
			} catch (Exception x) {
				result = defaultValue;
			}
		} else {
			result = defaultValue;
		}

		return result;
	}

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol,
			HttpServletRequest request) {
		// TODO Auto-generated method stub
		return new MessageInboundImpl(byteBufSize, charBufSize);
	}

	private final static List<MessageInbound> connections = new ArrayList<MessageInbound>();
	
	private static final class MessageInboundImpl extends MessageInbound {

		public MessageInboundImpl(int byteBufferMaxSize, int charBufferMaxSize) {
			super();
			setByteBufferMaxSize(byteBufferMaxSize);
			setCharBufferMaxSize(charBufferMaxSize);
		}

		@Override
		protected void onOpen(WsOutbound outbound) {
			connections.add(this);
			super.onOpen(outbound);
		}
		
		@Override
		protected void onClose(int status) {
			connections.remove(this);
			super.onClose(status);
		}
		
		@Override
		protected void onBinaryMessage(ByteBuffer message) throws IOException {
			throw new UnsupportedOperationException("Binary message not supported.");
		}

		@Override
		protected void onTextMessage(CharBuffer message) throws IOException {
			//getWsOutbound().writeTextMessage(message);
			broadcast(message.toString());
		}

		private void broadcast(String message) {
			for (MessageInbound connection : connections) {
				try {
					CharBuffer buffer = CharBuffer.wrap(message);
					connection.getWsOutbound().writeTextMessage(buffer);
				} catch (IOException ignore) {
					// Ignore
				}
			}
		}
	}
}
