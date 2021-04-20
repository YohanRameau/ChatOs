package fr.uge.chatos.frame;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

public class Public_msg extends SendToAll{

	private String sender;
	private String message;
	
	public Public_msg(String sender, String message) {
		super(sender);
		Objects.requireNonNull(message);
		this.message = message;
	}

	
	
	public String getSender() {
		return sender;
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
		var exp = BuildPacket.encodeString(sender);
		var msg = BuildPacket.encodeString(message);
		int bbSize = exp.remaining() + msg.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + 2 * Integer.BYTES + MAX_MESSAGE_SIZE + MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Message too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.PUBLIC_MSG.opCode);
		bb.put(exp);
		bb.put(msg);
		bb.flip();
		return bb;
	}

		
	
}
