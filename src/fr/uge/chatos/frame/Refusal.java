package fr.uge.chatos.frame;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

public class Refusal implements Frame{

	private String sender;
	
	public Refusal(String sender) {
		Objects.requireNonNull(sender);
		this.sender = sender;
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);		
	}

	@Override
	public ByteBuffer encode() {
		
		var senderBb = UTF8.encode(sender);
		int senderBbSize = senderBb.remaining();
		var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + senderBbSize);
		bb.put(PacketTypes.REFUSAL.opCode).putInt(senderBbSize).put(senderBb);
		bb.flip();
		return bb;
	}
	
}
