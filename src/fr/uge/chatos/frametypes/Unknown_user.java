package fr.uge.chatos.frametypes;

import java.nio.ByteBuffer;

import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.PacketTypes;
import fr.uge.chatos.visitor.FrameVisitor;

public class Unknown_user implements Frame{

	private String sender;
	
	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
	}
	
	public Unknown_user(String login) {
		sender = login;
	}

	@Override
	public ByteBuffer encode() {
		var exp = UTF8.encode(sender);
		int bbSize = Integer.BYTES + exp.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + Integer.BYTES + MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Packet too big to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.UNKNOWN_USER.ordinal());
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.flip();
		return bb;
	}

}
