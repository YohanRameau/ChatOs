package fr.uge.chatos.frametypes;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.PacketTypes;
import fr.uge.chatos.visitor.FrameVisitor;

public class Login_private implements Frame{

	private long id;
	
	
	public Login_private(long id) {
		Objects.requireNonNull(id);
		this.id = id;
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
		
	}

	@Override
	public ByteBuffer encode() {
		int bbSize = Long.BYTES + Byte.BYTES;
		if (bbSize > Byte.BYTES + Long.BYTES) {
			throw new IllegalStateException("Message or login too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.LOGIN_PRIVATE.opCode);
		bb.putLong(id);
		bb.flip();
		return bb;
	}
	
	
}
