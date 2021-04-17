package fr.uge.chatos.frame;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

public class Accept_co_private implements Frame{

	private String sender;
	private String receiver;
	
	public Accept_co_private(String sender, String receiver) {
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


	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ByteBuffer encode() {
		var exp = BuildPacket.encodeString(sender);
		var rec = BuildPacket.encodeString(receiver);
		int bbSize = exp.remaining() + rec.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + 2 * Integer.BYTES + 2 * MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Login too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put(PacketTypes.ACCEPT_CO_PRIVATE.opCode);
		bb.put(exp);
		bb.put(rec);
		bb.flip();
		return bb;
	}
	
}
