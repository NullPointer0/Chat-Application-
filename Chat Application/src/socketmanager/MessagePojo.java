package socketmanager;

import java.util.Date;

import socketmanager.SocketManager.MessageStatus;

public class MessagePojo extends AddSocket {
	private String messageId;
	private String messageText;
	private Integer status;
	private Long createdOn;

	public MessagePojo() {
	}

	public MessagePojo(String userId, String adminId, int socketBy, String messageText, Integer status) {
		super(userId, adminId, socketBy);
		this.createdOn = new Date().getTime();
		this.messageId = String.valueOf(createdOn);
		this.messageText = messageText;
		this.status = status == null ? MessageStatus.SENT.status : status;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getMessageText() {
		return messageText;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

}
