package fr.uge.chatos;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ClientList {

	private final Map<String, SocketChannel> clients = new HashMap<String, SocketChannel>();
	
	public boolean isPresent(String client) {
		return clients.containsKey(client);
	}
	
	public void add(String client, SocketChannel sc) {
		clients.putIfAbsent(client, sc);
	}

	@Override
	public String toString() {
		return "ClientList [clients=" + clients + "]";
	}
	
	
}