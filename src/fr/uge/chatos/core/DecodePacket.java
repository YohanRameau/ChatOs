package fr.uge.chatos.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class DecodePacket {
	
	/**
	 * Ensure that ByteBuffer have size bytes
	 * 
	 * @param sc -> Client SocketChannel 
	 * @param bb -> ByteBuffer containing readed bytes
	 * @param size -> size of bytes to read
	 * @throws IOException error if the client disconnect suddenly
	 */
	static boolean ensure(SocketChannel sc, ByteBuffer bb, int size) throws IOException {
		assert (size<=bb.capacity());
		while(bb.remaining()<size) {
			bb.compact();
			try {
				if (sc.read(bb) == -1) {
					return false;
				}
			} finally {
				bb.flip();
			}
		}
		return true;
	}
	
	public boolean server_acceptance(SocketChannel sc, ByteBuffer bb) throws IOException {
		if (!ensure(sc,bb,1))
			throw new IOException("Bad packet");
		var answer = bb.get();
		return answer == 1;
	}
		
}
