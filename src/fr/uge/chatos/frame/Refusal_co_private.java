package fr.uge.chatos.frame;

import java.nio.ByteBuffer;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

public class Refusal_co_private extends SendToOne{

	private String sender;
	private String receiver;
	
	public Refusal_co_private(String sender, String receiver) {
		super(sender, receiver);
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
		bb.put(PacketTypes.REFUSAL_CO_PRIVATE.opCode);
		bb.put(exp);
		bb.put(rec);
		bb.flip();
		return bb;
	}

}
