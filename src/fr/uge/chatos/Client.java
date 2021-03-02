package fr.uge.chatos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import fr.uge.chatos.core.BuildPacket;

public class Client {

	private final String name;
	private final SocketChannel sc;
	private final SocketChannel prv;
	private final ByteBuffer read;
	private final ByteBuffer write;
	private static final int BUFFER_SIZE = 1024;
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	
	public Client(String name) throws IOException {
		this.name = Objects.requireNonNull(name);
		this.sc = SocketChannel.open();
		this.prv = SocketChannel.open();
		this.read = ByteBuffer.allocateDirect(BUFFER_SIZE);
		this.write = ByteBuffer.allocateDirect(BUFFER_SIZE);
		sc.configureBlocking(false);
	}
	
	public void connexion(InetSocketAddress serv, String name) throws IOException {
		sc.connect(serv);
		BuildPacket.request_co_server(write, name);
		
	}
	
	public void close() throws IOException {
		sc.close();
		prv.close();
	}
}
