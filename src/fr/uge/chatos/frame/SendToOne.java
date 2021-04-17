package fr.uge.chatos.frame;

import java.util.Objects;

import fr.uge.chatos.core.Frame;

public abstract class SendToOne implements Frame{
	
	private String sender;
	private String receiver;
	
	public SendToOne(String sender, String receiver) {
		Objects.requireNonNull(sender);
		Objects.requireNonNull(receiver);
		this.sender = sender;
		this.receiver = receiver;
	}
	
	public String getSender() {
		return sender;
	}
	public String getReceiver() {
		return receiver;
	}
	
}
