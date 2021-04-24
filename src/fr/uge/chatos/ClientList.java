package fr.uge.chatos;

import java.util.HashMap;
import java.util.Map;

import fr.uge.chatos.context.ClientContext;
import fr.uge.chatos.context.ServerContext;

public class ClientList {

	private final Map<String, ServerContext> clients = new HashMap<String, ServerContext>();
	
	/**
	 * Determine if a client is already connected to the server
	 * 
	 * @param client the login to check
	 * @return boolean true if present, false else
	 */
	public boolean isPresent(String client) {
		for (String keys : clients.keySet())
		{
			if(keys.equals(client)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add a client to the connected list if not already present
	 * 
	 * @param client the login to add
	 * @param ctx the context to add to the map
	 */
	public void add(String client, ServerContext ctx) {
		clients.putIfAbsent(client, ctx);
	}
	
	/**
	 * Remove a client of the connected list
	 * 
	 * @param client the login to remove
	 */
	public void remove(String client) {
		clients.remove(client);
	}

	@Override
	public String toString() {
		return "ClientList [clients=" + clients + "]";
	}
	
}