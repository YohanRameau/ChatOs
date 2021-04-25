package fr.uge.chatos.frametypes;

import java.nio.ByteBuffer;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.PacketTypes;
import fr.uge.chatos.visitor.FrameVisitor;

public class Request_co_private extends SendToOne{

	
	public Request_co_private(String sender, String receiver) {
		super(sender, receiver);
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);		
	}

	@Override
	public ByteBuffer encode() {
		var exp = BuildPacket.encodeString(getSender());
		var rec = BuildPacket.encodeString(getReceiver());
		int bbSize = exp.remaining() + rec.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + 2 * Integer.BYTES + 2 * MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Login too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put(PacketTypes.REQUEST_CO_PRIVATE.opCode);
		bb.put(exp);
		bb.put(rec);
		bb.flip();
		return bb;
	}

}
