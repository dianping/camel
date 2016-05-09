package com.dianping.phoenix.lb.monitor;

// "name": "10.1.1.79:80", "status": "up", "rise": 9107, "fall": 0, "type": "tcp", "port": 0
public class NodeStatus {

	private String name;

	private Status status;

	private long rise;

	private long fall;

	private String type;

	private int port;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public long getRise() {
		return rise;
	}

	public void setRise(long rise) {
		this.rise = rise;
	}

	public long getFall() {
		return fall;
	}

	public void setFall(long fall) {
		this.fall = fall;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return String
				.format("NodeStatus [name=%s, status=%s, rise=%s, fall=%s, type=%s, port=%s]", name, status, rise, fall,
						type, port);
	}

	public static enum Status {
		up, down;

	}

}
