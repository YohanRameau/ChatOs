package fr.uge.chatos.core;

import java.nio.BufferUnderflowException;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.chatos.packetreader.Packet;

public class BuildPacket {

	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final int MAX_NICKNAME_SIZE = 24;
	private static final int MAX_MESSAGE_SIZE = 512;

	public static ByteBuffer encode(Packet pck) {
		switch (pck.getOpCode()) {
		case 1: {
			var senderBb = UTF8.encode(pck.getSender());
			int senderBbSize = senderBb.remaining();
			var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + senderBbSize);
			bb.put((byte) 1).putInt(senderBbSize).put(senderBb);
			bb.flip();
			return bb;
		}
		case 2: {
			var senderBb = UTF8.encode(pck.getSender());
			int senderBbSize = senderBb.remaining();
			//System.out.println("encode " + senderBbSize + " " + pck.getSender());
			var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + senderBbSize);
			bb.put((byte) 2).putInt(senderBbSize).put(senderBb);
			bb.flip();
			return bb;
		}
		case 3: {
			var senderBb = UTF8.encode(pck.getSender());
			int senderBbSize = senderBb.remaining();
			var receiverBb = UTF8.encode(pck.getReceiver());
			int receiverBbSize = receiverBb.remaining();
			var bb = ByteBuffer.allocate(Byte.BYTES + 2 * Integer.BYTES + senderBbSize + receiverBbSize);
			bb.put((byte) 3).putInt(senderBbSize).put(senderBb).putInt(receiverBbSize).put(receiverBb);
			bb.flip();
			return bb;
		}
		case 4: {
			var senderBb = UTF8.encode(pck.getSender());
			int senderBbSize = senderBb.remaining();
			var messageBb = UTF8.encode(pck.getMessage());
			int messageBbSize = messageBb.remaining();
			var bb = ByteBuffer.allocate(Byte.BYTES + 2 * Integer.BYTES + senderBbSize + messageBbSize);
			bb.put((byte) 4).putInt(senderBbSize).put(senderBb).putInt(messageBbSize).put(messageBb);
			bb.flip();
			return bb;
		}
		case 5: {
			var senderBb = UTF8.encode(pck.getSender());
			int senderBbSize = senderBb.remaining();
			var receiverBb = UTF8.encode(pck.getReceiver());
			int receiverBbSize = receiverBb.remaining();
			var messageBb = UTF8.encode(pck.getMessage());
			int messageBbSize = messageBb.remaining();
			var bb = ByteBuffer.allocate(Byte.BYTES + 3 * Integer.BYTES + senderBbSize + receiverBbSize + messageBbSize);
			bb.put((byte) 5).putInt(senderBbSize).put(senderBb).putInt(receiverBbSize).put(receiverBb).putInt(messageBbSize).put(messageBb);
			bb.flip();
			return bb;
		}
		case 6: {
			var senderBb = UTF8.encode(pck.getSender());
			int senderBbSize = senderBb.remaining();
			var bb = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + senderBbSize);
			bb.put((byte) 6).putInt(senderBbSize).put(senderBb);
			bb.flip();
			return bb;
		}
		default:
			throw new IllegalStateException("The Opcode " + pck.getOpCode() + " is not defined for ChatOs protocol.");
		}

	}

	/**
	 * Build a packet asking for a client->server connection
	 * 
	 * @param bb   -> ByteBuffer used for sending bytes
	 * @param name -> client's nickname
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static void request_co_server(ByteBuffer bb, String name) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES + Integer.BYTES + MAX_NICKNAME_SIZE) {
			throw new BufferUnderflowException();
		}
		;
		bb.put((byte) PacketTypes.REQUEST_CO_SERVER.ordinal());
		var nickname = UTF8.encode(name);
		bb.putInt(nickname.remaining());
		bb.put(nickname);
	}

	/**
	 * Build a packet meaning connection acceptance
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static void acceptance(ByteBuffer bb) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES) {
			throw new BufferUnderflowException();
		}
		;
		bb.put((byte) PacketTypes.ACCEPTANCE.ordinal());
	}

	/**
	 * Build a packet meaning connection refusal
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static void refusal(ByteBuffer bb) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES) {
			throw new BufferUnderflowException();
		}
		;
		bb.put((byte) PacketTypes.REFUSAL.ordinal());
	}

	/**
	 * Build a packet asking for a client->client connection
	 * 
	 * @param bb       -> ByteBuffer used for sending bytes
	 * @param sender   -> sender client's nickname
	 * @param receiver -> receiver client's nickname
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static void request_co_user(ByteBuffer bb, String sender, String receiver) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES + 2 * Integer.BYTES + 2 * MAX_NICKNAME_SIZE) {
			throw new BufferUnderflowException();
		}
		;
		bb.put((byte) PacketTypes.REQUEST_CO_USER.ordinal());
		var exp = UTF8.encode(sender);
		var dest = UTF8.encode(receiver);
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.putInt(dest.remaining());
		bb.put(dest);
	}

	/**
	 * Build a packet representing a public message and send on read-mode.
	 * 
	 * @param sender  -> sender client's nickname
	 * @param message -> the message to send
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer public_msg(String sender, String message) {
		var exp = UTF8.encode(sender);
		var msg = UTF8.encode(message);
		int bbSize = 2 * Integer.BYTES + exp.remaining() + msg.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + 2 * Integer.BYTES + MAX_MESSAGE_SIZE + MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Message too long to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.PUBLIC_MSG.ordinal());
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.putInt(msg.remaining());
		bb.put(msg);
		bb.flip();
		return bb;
	}

	/**
	 * Build a packet representing a private message
	 * 
	 * @param sender   -> sender client's nickname
	 * @param receiver -> receiver client's nickname
	 * @param message  -> the message to send
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer private_msg(String sender, String receiver, String message) {
		var exp = UTF8.encode(sender);
		var dest = UTF8.encode(receiver.substring(1));
		var msg = UTF8.encode(message);
		int bbSize = 3 * Integer.BYTES + exp.remaining() + dest.remaining() + msg.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + 3 * Integer.BYTES + 2 * MAX_NICKNAME_SIZE + MAX_MESSAGE_SIZE) {
			throw new BufferUnderflowException();
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.PRIVATE_MSG.ordinal());
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.putInt(dest.remaining());
		bb.put(dest);
		bb.putInt(msg.remaining());
		bb.put(msg);
		bb.flip();
		return bb;
	}
	
	/**
	 * Build a packet meaning the user doesn't exist
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer unknown_user(String sender) {
		var exp = UTF8.encode(sender);
		int bbSize = Integer.BYTES + exp.remaining() + Byte.BYTES;
		if (bbSize > Byte.BYTES + Integer.BYTES + MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Packet too big to be send on the server.");
		}
		ByteBuffer bb = ByteBuffer.allocate(bbSize);
		bb.put((byte) PacketTypes.UNKNOWN_USER.ordinal());
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.flip();
		return bb;
	}

}
