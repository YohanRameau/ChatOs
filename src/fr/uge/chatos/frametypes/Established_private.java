package fr.uge.chatos.frametypes;

import java.nio.ByteBuffer;

import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.PacketTypes;
import fr.uge.chatos.visitor.FrameVisitor;

public class Established_private implements Frame{
	
	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ByteBuffer encode() {
		int bbSize = Byte.BYTES;
		if (bbSize > Byte.BYTES) {
			throw new IllegalStateException("Message or login too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.ESTABLISHED_PRIVATE.opCode);
		bb.flip();
		return bb;
	}
	
}
