package fr.uge.chatos.core;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BuildPacket {

	private static final Charset UTF8 = StandardCharsets.UTF_8;
	
	/**
	 * Encode a string and his size into a ByteBuffer
	 * 
	 * @param string The string to be encoded
	 * @return bb The returned ByteBuffer
	 */
	public static ByteBuffer encodeString(String string) {
		var stringBb = UTF8.encode(string);
		int senderBbSize = stringBb.remaining();
		var bb = ByteBuffer.allocate(Integer.BYTES + senderBbSize);
		bb.putInt(senderBbSize);
		bb.put(stringBb);
		bb.flip();
		return bb;
	}	
}
