package socketmanager;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.util.internal.StringUtil;

public class SocketManager implements ConnectListener, DisconnectListener {

	private SocketIOServer socketIOServer;
	private Map<String, SocketIOClient> userVsIOClientMap;
	private Map<String, SocketIOClient> adminVsSocketIOClient;
	private ObjectMapper objectMapper;
	private static final Integer PORT = 3003;

	@PostConstruct
	public void init() {
		userVsIOClientMap = new ConcurrentHashMap<>();
		adminVsSocketIOClient = new ConcurrentHashMap<>();
		Configuration configuration = new Configuration();
		configuration.setPort(PORT);
		socketIOServer = new SocketIOServer(configuration);
		socketIOServer.addConnectListener(this);
		socketIOServer.addDisconnectListener(this);
		socketIOServer.addEventListener(SocketEvent.ADD_SOCKET.getEventName(), String.class, addSocketListener);
		socketIOServer.addEventListener(SocketEvent.RECEIVE_MESSAGE.getEventName(), String.class, messageListener);
		// socketIOServer.addEventListener(SocketEvent.CHANGE_STATUS.getEventName(),
		// String.class, onStatusChangeListener);
		socketIOServer.start();
		objectMapper = new ObjectMapper();
	}

	@Override
	public void onDisconnect(SocketIOClient client) {
		Iterator<Map.Entry<String, SocketIOClient>> userIterator = userVsIOClientMap.entrySet().iterator();
		while (userIterator.hasNext()) {
			Map.Entry<String, SocketIOClient> userIdVsClientEntry = userIterator.next();
			SocketIOClient registeredClient = userIdVsClientEntry.getValue();
			if (registeredClient != null && client.getSessionId().equals(registeredClient.getSessionId())) {
				userIterator.remove();
				client.disconnect();
				return;
			}
		}

		Iterator<Map.Entry<String, SocketIOClient>> adminIterator = adminVsSocketIOClient.entrySet().iterator();
		while (adminIterator.hasNext()) {
			Map.Entry<String, SocketIOClient> userIdVsClientEntry = adminIterator.next();
			SocketIOClient registeredClient = userIdVsClientEntry.getValue();
			if (registeredClient != null && client.getSessionId().equals(registeredClient.getSessionId())) {
				adminIterator.remove();
				client.disconnect();
				return;
			}
		}
	}

	@Override
	public void onConnect(SocketIOClient arg0) {

	}

	private DataListener<String> addSocketListener = new DataListener<String>() {
		public void onData(SocketIOClient client, String response, AckRequest ackSender) throws Exception {
			AddSocket data = objectMapper.readValue(response, AddSocket.class);
			if (data != null) {
				ToldBy toldBy = ToldBy.findByType(data.getSocketBy());
				if (toldBy != null) {
					switch (toldBy) {
					case BY_USER:
						String userId = data.getUserId();
						if (!StringUtil.isNullOrEmpty(userId))
							userVsIOClientMap.put(userId, client);
						break;
					case BY_HOTEL:
						String adminId = data.getAdminId();
						if (!StringUtil.isNullOrEmpty(adminId))
							adminVsSocketIOClient.put(adminId, client);
						break;
					default:
						break;
					}
				}
			}
		}
	};

	private DataListener<String> messageListener = new DataListener<String>() {
		@Override
		public void onData(SocketIOClient client, String response, AckRequest ackSender) throws Exception {
			System.out.println("3. In message listener");
			System.out.println("Response " + response);
			MessagePojo data = objectMapper.readValue(response, MessagePojo.class);
			if (data != null) {
				String userId = data.getUserId();
				String adminId = data.getAdminId();
				ToldBy toldBy = ToldBy.findByType(data.getSocketBy());
				MessageStatus messageStatus = null;
				SocketIOClient socketIOClient;
				if (toldBy != null) {
					switch (toldBy) {
					case BY_USER: {
						if (adminVsSocketIOClient.containsKey(adminId)) {
							messageStatus = MessageStatus.SENT;
							socketIOClient = adminVsSocketIOClient.get(adminId);
							socketIOClient.sendEvent(SocketEvent.SEND_MESSAGE.getEventName(), data);
						}
						data.setStatus(messageStatus.getStatus());
						saveMessage(data);
						client.sendEvent(SocketEvent.ACKNOWLEDGEMENT.getEventName(), messageStatus.status);
						break;
					}
					case BY_HOTEL: {
						if (adminVsSocketIOClient.containsKey(userId)) {
							messageStatus = MessageStatus.SENT;
							socketIOClient = adminVsSocketIOClient.get(adminId);
							socketIOClient.sendEvent(SocketEvent.SEND_MESSAGE.getEventName(), data);
						}
						data.setStatus(messageStatus.getStatus());
						saveMessage(data);
						client.sendEvent(SocketEvent.ACKNOWLEDGEMENT.getEventName(), messageStatus.status);
						break;
					}
					default:
						break;
					}
				}
			}
		}

		private void saveMessage(MessagePojo data) {
			// TODO Auto-generated method stub

		}

	};

	private enum SocketEvent {
		ADD_SOCKET("addSocket", "addSocket"), RECEIVE_MESSAGE("receive", "receive"), SEND_MESSAGE("send", "send"),
		CHANGE_STATUS("changeStatus", "changeStatus"), ACKNOWLEDGEMENT("acknowledge", "acknowledge");
		private String eventName;

		public String getEventName() {
			return eventName;
		}

		private SocketEvent(String eventName, String massageType) {
			this.eventName = eventName;
		}
	}

	public enum ToldBy {
		BY_USER(1), BY_HOTEL(2), REQUEST_AUTOMATED(3);
		int type;

		private ToldBy(int type) {
			this.type = type;
		}

		public static ToldBy findByType(int type) {
			for (ToldBy toldBy : values()) {
				if (toldBy.getType() == type)
					return toldBy;
			}
			return null;
		}

		public int getType() {
			return type;
		}
	}

	public enum MessageStatus {
		SENT(1), DELIVERED(2), READ(3);
		int status;

		private MessageStatus(int type) {
			this.status = type;
		}

		public static MessageStatus findByType(int code) {
			for (MessageStatus status : values()) {
				if (status.getStatus() == code)
					return status;
			}
			return null;
		}

		public int getStatus() {
			return status;
		}
	}
}
