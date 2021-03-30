package fr.uge.chatos;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ClientList {

	private final Map<String, SocketChannel> clients = new HashMap<String, SocketChannel>();
	
	public boolean isPresent(String client) {
		return clients.containsKey(client);
	}
	
}
