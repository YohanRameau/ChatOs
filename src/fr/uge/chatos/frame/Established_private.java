package fr.uge.chatos.frame;

import java.nio.ByteBuffer;

import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

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
