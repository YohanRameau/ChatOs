package fr.uge.chatos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.chatos.context.Context;
import fr.uge.chatos.context.ServerContext;
import fr.uge.chatos.frametypes.SendToAll;
import fr.uge.chatos.frametypes.SendToOne;

public class Server {

	static class PrivateConnectionInfo {
		private final long id;
		private final InetSocketAddress firstAddress;
		private final InetSocketAddress secondAddress;
		private Context firstContext;
		private Context secondContext;

		public PrivateConnectionInfo(long id, InetSocketAddress frst, InetSocketAddress scnd) {
			this.id = id;
			this.firstAddress = frst;
			this.secondAddress = scnd;
		}
	}

	static private final int BUFFER_SIZE = 1_024;
	static private final Logger logger = Logger.getLogger(Server.class.getName());

	private final AtomicLong privateConnectionCompt = new AtomicLong();
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final ClientList clientList = new ClientList();

	private final Map<Long, PrivateConnectionInfo> PrivateConnectionMap = new HashMap<>();

	public Server(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		while (!Thread.interrupted()) {
			printKeys(); // for debug
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}

	public long generateId() {
		return privateConnectionCompt.getAndIncrement();
	}

	/**
	 * 
	 * @param key
	 */
	private void treatKey(SelectionKey key) {
		printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "Connection closed with client due to IOException", e);
			silentlyClose(key);
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		var sc = serverSocketChannel.accept();
		if (sc != null) {
			sc.configureBlocking(false);
			var clientKey = sc.register(selector, SelectionKey.OP_READ);
			clientKey.attach(new ServerContext(this, clientKey, clientList));
		} else {
			logger.warning("The selector was wrong.");
		}
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}
	
	public void disconnect(String login) {
		clientList.remove(login);
	}
	
//	public boolean registerPrivateConnection(long id, ) {
//		PrivateConnectionMap.put(key, value)
//	}
	
	/**
	 * Add a message to all connected clients queue
	 *
	 * @param msg
	 */
	public void broadcast(SendToAll packet) {
		for (var key : selector.keys()) {
			var context = (ServerContext) key.attachment();
			if (context == null || context.privateConnection() == true) {
				continue;
			}
			context.queueMessage(packet);
		}
	}

	/**
	 * Add a message to a particulary connected client queue
	 *
	 * @param msg
	 */
	public boolean unicast(SendToOne pck) {
		if (clientList.isPresent(pck.getReceiver())) {
			for (var key : selector.keys()) {
				var context = (ServerContext) key.attachment();
				if (context == null)
					continue;
				if (context.isClient(pck.getReceiver())) {
					context.queueMessage(pck);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean privateUnicast(SendToOne packet) {
		if (clientList.isPresent(packet.getReceiver())) {
			for (var key : selector.keys()) {
				var context = (ServerContext) key.attachment();
				if (context == null)
					continue;
				if (context.isClient(packet.getReceiver()) && context.addRequester(packet.getSender())) {
					context.queueMessage(packet);
					return true;
				}
				if (context.isClient(packet.getReceiver()) && !context.addRequester(packet.getSender())){
					// TODO
					// Envoyer message d'erreur parce que déjà demandé ou ignorer la demande 
					return true;
				}
			}
		}
		return false;
	}

	// Ajout d'une méthode unicast-like pour envoyer des réponses uniquement aux
	// interessés

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		new Server(Integer.parseInt(args[0])).launch();
	}

	private static void usage() {
		System.out.println("Usage : ServerSumBetter port");
	}

	////////////////// DEBUG //////////////////////

	/***
	 * Theses methods are here to help understanding the behavior of the selector
	 ***/

	private String interestOpsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps & SelectionKey.OP_ACCEPT) != 0)
			list.add("OP_ACCEPT");
		if ((interestOps & SelectionKey.OP_READ) != 0)
			list.add("OP_READ");
		if ((interestOps & SelectionKey.OP_WRITE) != 0)
			list.add("OP_WRITE");
		return String.join("|", list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : " + interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
			}
		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e) {
			return "???";
		}
	}

	public void printSelectedKey(SelectionKey key) {
		SelectableChannel channel = key.channel();
		if (channel instanceof ServerSocketChannel) {
			System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
		} else {
			SocketChannel sc = (SocketChannel) channel;
			System.out.println(
					"\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable())
			list.add("ACCEPT");
		if (key.isReadable())
			list.add("READ");
		if (key.isWritable())
			list.add("WRITE");
		return String.join(" and ", list);
	}
}
