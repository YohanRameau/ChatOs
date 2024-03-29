package fr.uge.chatos.frametypes;

import java.nio.ByteBuffer;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.PacketTypes;
import fr.uge.chatos.visitor.FrameVisitor;

public class PrivateConnectionMessage implements Frame{
	
	private final String message;
	private final long id;
	
	public PrivateConnectionMessage(String message, long id) {
		this.message = message;
		this.id = id;
	}
	
	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
	}

	
	public long getId() {
		return id;
	}
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public ByteBuffer encode() {
		var msg = BuildPacket.encodeString(message);
		int bbSize = msg.remaining() + Byte.BYTES + Long.BYTES;
		if (bbSize > Byte.BYTES + Integer.BYTES + Long.BYTES + MAX_MESSAGE_SIZE) {
			throw new IllegalStateException("Message too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.PRIVATE_CO_MSG.opCode);
		bb.putLong(id);
		bb.put(msg);
		bb.flip();
		return bb;
	}

}
