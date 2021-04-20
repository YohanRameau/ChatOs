package fr.uge.chatos.frame;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

public class Id_private extends SendToOne{

	private long id;
	
	public Id_private(String sender, String receiver, long id) {
		super(sender, receiver);
		Objects.requireNonNull(id);
		this.id = id;
	}
	
	public long getId() {
		return id;
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);	
	}

	@Override
	public ByteBuffer encode() {
		var exp = BuildPacket.encodeString(super.getSender());
		var rec = BuildPacket.encodeString(super.getReceiver());
		
		int bbSize =  exp.remaining() + rec.remaining() + Long.BYTES + Byte.BYTES;
		if (bbSize > Byte.BYTES + 2 * Integer.BYTES + 2 * MAX_NICKNAME_SIZE + Long.BYTES) {
			throw new IllegalStateException("Message or login too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.ID_PRIVATE.opCode);
		bb.put(exp);
		bb.put(rec);
		bb.putLong(id);
		bb.flip();
		return bb;
	}

	
	
}
