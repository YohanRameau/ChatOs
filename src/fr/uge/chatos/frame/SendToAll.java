package fr.uge.chatos.frame;

import java.util.Objects;

import fr.uge.chatos.core.Frame;

public abstract class SendToAll implements Frame{

	private String sender;
	
	public SendToAll(String sender) {
		Objects.requireNonNull(sender);
		this.sender = sender;
	}

	public String getSender() {
		return sender;
	}
	
}
