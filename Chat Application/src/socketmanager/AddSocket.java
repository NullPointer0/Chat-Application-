package socketmanager;

import java.util.Objects;

public class AddSocket {
	private String userId;
	private String adminId;
	private int socketBy;

	public AddSocket() {
	}

	public AddSocket(String userId, String adminId, int socketBy) {
		super();
		this.userId = Objects.requireNonNull(userId, "user id can't be null");
		this.adminId = Objects.requireNonNull(adminId, "admin id can't be null");
		this.socketBy = socketBy;
	}

	public String getUserId() {
		return userId;
	}

	public String getAdminId() {
		return adminId;
	}

	public int getSocketBy() {
		return socketBy;
	}

}
