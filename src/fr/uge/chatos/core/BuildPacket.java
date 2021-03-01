package fr.uge.chatos.core;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BuildPacket{
	
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final int MAX_NICKNAME_SIZE = 24;
	private static final int MAX_MESSAGE_SIZE = 512;
	
	enum Types {
	    REQUEST_CO_SERVER,
	    ACCEPTANCE,
	    REFUSAL,
	    REQUEST_CO_USER,
	    PUBLIC_MSG,
	    PRIVATE_MSG
	}
	
	
	/**
	 * Build a packet asking for a client->server connection
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @param name -> client's nickname
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public void request_co_server(ByteBuffer bb, String name) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES+Integer.BYTES+MAX_NICKNAME_SIZE) {
			throw new BufferUnderflowException();
		};
		bb.put((byte) Types.REQUEST_CO_SERVER.ordinal());
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
	public void acceptance(ByteBuffer bb) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES) {
			throw new BufferUnderflowException();
		};
		bb.put((byte) Types.ACCEPTANCE.ordinal());
	}
	
	/**
	 * Build a packet meaning connection refusal
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public void refusal(ByteBuffer bb) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES) {
			throw new BufferUnderflowException();
		};
		bb.put((byte) Types.REFUSAL.ordinal());
	}
	
	/**
	 * Build a packet asking for a client->client connection
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @param sender -> sender client's nickname
	 * @param receiver -> receiver client's nickname
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public void request_co_user(ByteBuffer bb, String sender, String receiver) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES+2*Integer.BYTES+2*MAX_NICKNAME_SIZE) {
			throw new BufferUnderflowException();
		};
		bb.put((byte) Types.REQUEST_CO_USER.ordinal());
		var exp = UTF8.encode(sender);
		var dest = UTF8.encode(receiver);
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.putInt(dest.remaining());
		bb.put(dest);
	}
	
	/**
	 * Build a packet representing a public message
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @param sender -> sender client's nickname
	 * @param message -> the message to send
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public void public_msg(ByteBuffer bb, String sender, String message) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES+2*Integer.BYTES+MAX_MESSAGE_SIZE+MAX_NICKNAME_SIZE) {
			throw new BufferUnderflowException();
		};
		bb.put((byte) Types.PUBLIC_MSG.ordinal());
		var exp = UTF8.encode(sender);
		var msg = UTF8.encode(message);
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.putInt(msg.remaining());
		bb.put(msg);
	}
	
	/**
	 * Build a packet representing a private message
	 * 
	 * @param bb -> ByteBuffer used for sending bytes
	 * @param sender -> sender client's nickname
	 * @param receiver -> receiver client's nickname
	 * @param message -> the message to send
	 * @throws BufferUnderFlow error if the ByteBuffer size is too small
	 */
	public void private_msg(ByteBuffer bb, String sender, String receiver, String message) {
		bb.clear();
		if (bb.remaining() < Byte.BYTES+3*Integer.BYTES+2*MAX_NICKNAME_SIZE+MAX_MESSAGE_SIZE) {
			throw new BufferUnderflowException();
		};
		bb.put((byte) Types.PUBLIC_MSG.ordinal());
		var exp = UTF8.encode(sender);
		var dest = UTF8.encode(receiver);
		var msg = UTF8.encode(message);
		bb.putInt(exp.remaining());
		bb.put(exp);
		bb.putInt(dest.remaining());
		bb.put(dest);
		bb.putInt(msg.remaining());
		bb.put(msg);
	}

}
