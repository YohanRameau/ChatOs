package fr.uge.chatos.frame;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

public class Private_msg extends SendToOne{

	private String message;
	
	
	public Private_msg(String sender, String receiver, String message) {
		super(sender, receiver);
		Objects.requireNonNull(message);
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);		
	}

	@Override
	public ByteBuffer encode() {
		var exp = BuildPacket.encodeString(getSender());
		var rec = BuildPacket.encodeString(getReceiver());
		var msg = BuildPacket.encodeString(message);
		
		int bbSize =  exp.remaining() + rec.remaining() + msg.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + 3 * Integer.BYTES + 2 * MAX_NICKNAME_SIZE + MAX_MESSAGE_SIZE) {
			throw new IllegalStateException("Message or login too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.PRIVATE_MSG.opCode);
		bb.put(exp);
		bb.put(rec);
		bb.put(msg);
		bb.flip();
		return bb;
	}

}
