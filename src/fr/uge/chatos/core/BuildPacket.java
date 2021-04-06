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
			var bb = ByteBuffer.allocate(Byte.BYTES + 3 * Integer.BYTES + senderBbSize + messageBbSize);
			bb.put((byte) 5).putInt(senderBbSize).put(senderBb).putInt(receiverBbSize).put(receiverBb).putInt(messageBbSize).put(messageBb);
			bb.flip();
			return bb;
		}
		default:
			throw new IllegalStateException("The Opcode " + pck.getOpCode() + " is not defined for ChatOs protocol.");
		}

	}
	
	
	private static ByteBuffer encodeString(String string) {
		
		var stringBb = UTF8.encode(string);
		int senderBbSize = stringBb.remaining();
		var bb = ByteBuffer.allocate(Integer.BYTES + senderBbSize);
		bb.putInt(senderBbSize);
		bb.put(stringBb);
		bb.flip();
		return bb;
	}

	/**
	 * Build a packet asking for a client->server connection
	 * 
	 * @param bb   -> ByteBuffer used for sending bytes
	 * @param name -> client's nickname
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer request_co_server(String name) {
		
		var nicknameBuffer = encodeString(name);
		if (nicknameBuffer.remaining() > Integer.BYTES + MAX_NICKNAME_SIZE) {
			throw new IllegalStateException("Nickname too long to be send on the server.");
		}
		var bb = ByteBuffer.allocate(nicknameBuffer.remaining() + Byte.BYTES);
		bb.put((byte) PacketTypes.REQUEST_CO_SERVER.opCode);
		bb.put(nicknameBuffer);
		bb.flip();
		return bb;
	}

	/**
	 * Build a packet meaning connection acceptance
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer acceptance(String name) {
		ByteBuffer bb = ByteBuffer.allocate(1);
		bb.put((byte) PacketTypes.ACCEPTANCE.opCode);		
		bb.flip();
		return bb;
	}

	/**
	 * Build a packet meaning connection refusal
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer refusal() {
		ByteBuffer bb = ByteBuffer.allocate(1);
		bb.put((byte) PacketTypes.REFUSAL.opCode);
		bb.flip();
		return bb;
	}

	/**
	 * Build a packet asking for a client->client connection
	 * 
	 * @param bb       -> ByteBuffer used for sending bytes
	 * @param sender   -> sender client's nickname
	 * @param receiver -> receiver client's nickname
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer request_co_private(String sender, String receiver) {
		
		var exp = BuildPacket.encodeString(sender);
		var rec = BuildPacket.encodeString(receiver);
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

	/**
	 * Build a packet representing a public message and send on read-mode.
	 * 
	 * @param sender  -> sender client's nickname
	 * @param message -> the message to send
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer public_msg(String sender, String message) {
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

	/**
	 * Build a packet representing a private message
	 * 
	 * @param sender   -> sender client's nickname
	 * @param receiver -> receiver client's nickname
	 * @param message  -> the message to send
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public static ByteBuffer private_msg(String sender, String receiver, String message) {
		var exp = BuildPacket.encodeString(sender);
		var rec = BuildPacket.encodeString(receiver);
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
