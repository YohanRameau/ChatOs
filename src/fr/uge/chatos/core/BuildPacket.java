package fr.uge.chatos.core;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.chatos.framereader.Packet;

public class BuildPacket {

	private static final Charset UTF8 = StandardCharsets.UTF_8;
	
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
