package fr.uge.chatos;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Client {

	private final String name;
	private final SocketChannel sc;
	private SocketChannel prv;
	private final ByteBuffer read;
	private final ByteBuffer write;
	private static final int BUFFER_SIZE = 1024;
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	
	public Client(String name) {
		this.name = name;
		this.read = ByteBuffer.allocateDirect(BUFFER_SIZE);
		this.write = ByteBuffer.allocateDirect(BUFFER_SIZE);
	}
	
}
