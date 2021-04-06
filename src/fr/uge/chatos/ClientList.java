package fr.uge.chatos;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ClientList {

	private final Map<String, SocketChannel> clients = new HashMap<String, SocketChannel>();
	
	public boolean isPresent(String client) {
		for (String keys : clients.keySet())
		{
			if(keys.equals(client)) {
				return true;
			}
		}
		return false;
	}
	
	public void add(String client, SocketChannel sc) {
		clients.putIfAbsent(client, sc);
	}
	
	public void remove(String client) {
		clients.remove(client);
	}

	@Override
	public String toString() {
		return "ClientList [clients=" + clients + "]";
	}
	
}