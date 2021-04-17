package fr.uge.chatos.frame;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.FrameVisitor;
import fr.uge.chatos.core.PacketTypes;

public class Request_co_server implements Frame{

	private String sender;
	
	public Request_co_server(String sender) {
		Objects.requireNonNull(sender);
		this.sender = sender;
	}	
		
	
	public String getSender() {
		return sender;
	}


	@Override
	public void accept(FrameVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public ByteBuffer encode() {
		var nicknameBuffer = BuildPacket.encodeString(sender);
		if (nicknameBuffer.remaining() > Integer.BYTES + MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Nickname too long to be send on the server.");
		}
		var bb = ByteBuffer.allocate(nicknameBuffer.remaining() + Byte.BYTES);
		bb.put((byte) PacketTypes.REQUEST_CO_SERVER.opCode);
		bb.put(nicknameBuffer);
		bb.flip();
		return bb;
	}

}