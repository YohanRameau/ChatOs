package fr.uge.chatos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.chatos.context.Context;
import fr.uge.chatos.context.ServerContext;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.frametypes.SendToAll;
import fr.uge.chatos.frametypes.SendToOne;

public class Server {

	public static class PrivateConnectionsInformations {
		private final long id;
		private ServerContext firstContext = null;
		private ServerContext secondContext = null;

		public PrivateConnectionsInformations(long id) {
			this.id = id;
		}
		
		public boolean initialized() {
			return firstContext != null && secondContext != null;
		}
		
		public boolean registerClient(ServerContext ctx) {
			if(firstContext == null) {
				firstContext = ctx;
				return true;
			} 
			else if( secondContext == null) {
				secondContext = ctx;
				return true;
			}
			return false;
		}
		
		private Optional<ServerContext> getOtherContext(ServerContext sctx) {
			ServerContext result = null;			
			if(sctx == firstContext) {
				result = secondContext;
			} else if (sctx == secondContext ) {
				result =  firstContext;
			}
			return Optional.ofNullable(result);
		}
		
		public void edgeSending(ServerContext context, Frame frame) {
			var optional_ctx = getOtherContext(context);
			if(optional_ctx.isEmpty()) {
				// Usurpation connection
				context.silentlyClose();
			} 
			var other_ctx = optional_ctx.get();
			other_ctx.queueMessage(frame);
		}
	}

	static private final Logger logger = Logger.getLogger(Server.class.getName());
	static private final long TIMEOUT = 1000 * 60 * 5; // 5 minutes timeout 
	
	private long privateConnectionCompt = 0;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final ClientList clientList = new ClientList();

	private final Map<Long, PrivateConnectionsInformations> privateConnectionMap = new HashMap<>();

	public Server(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
	}

	/**
	 * Launch the server
	 */
	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		long startTimer = System.currentTimeMillis(); 
		while (!Thread.interrupted()) {		
			printKeys(); // for debug
			try {
				selector.select(this::treatKey, TIMEOUT);
				long timeSinceLastcheck = (System.currentTimeMillis() - startTimer) ;
				if(TIMEOUT < timeSinceLastcheck) {
					checkInactiveClient();
					startTimer = System.currentTimeMillis();
				}
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}
	
	
	public long generateId() {
		long res = privateConnectionCompt;
		privateConnectionCompt += 1; 
		return res;
	}
	
	public Optional<PrivateConnectionsInformations> getPrivateConnectionInfo(long id) {
		return Optional.ofNullable(privateConnectionMap.get(id));
	}
	
	public long initializedPrivateconnection() {
		var id = generateId();
		privateConnectionMap.put(id, new PrivateConnectionsInformations(id));
		return id;
	}
	
	public boolean registerPrivateConnection(long id, ServerContext ctx) {
		
		var clientInfo = privateConnectionMap.get(id);
		if(clientInfo == null) {

			ctx.silentlyClose();
			return false;
		}
		if(clientInfo.initialized()) {
			ctx.silentlyClose();
			return false;
		}
		
		return clientInfo.registerClient(ctx);
	}
	
	/**
	 * 
	 * @param key
	 */
	private void disconnectIfInactive(SelectionKey key) {
		
		var ctx = (ServerContext) key.attachment();
		if(ctx == null) {
			return;
		} 
		if(!ctx.isActive() || ctx.fulledQueue()) {
			ctx.silentlyClose();
		} else {
			ctx.setInactive();
		}
	}
	
	private void checkInactiveClient() {
		selector.keys().stream().forEach(this::disconnectIfInactive); 
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
				((ServerContext) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((ServerContext) key.attachment()).doRead();
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
		
		var optionalClient = clientList.getClient(pck.getReceiver());
		if(optionalClient.isEmpty()) {
			return false;
		}
		optionalClient.get().queueMessage(pck);
		return true;
	}
	
	
	public boolean privateConnectionInit(ServerContext senderContext, SendToOne packet) {
		var sender = packet.getSender();
		var receiver = packet.getReceiver();
		var optionalClient = clientList.getClient(receiver);
		if(optionalClient.isEmpty()) {
			return false;
		}
		var receiverContext = optionalClient.get();		
		if(receiverContext.addRequester(sender)) {
			senderContext.addRequester(receiver);
			receiverContext.queueMessage(packet);
			return true;
		}
		return true;
		
	}
	
	/**
	 * 
	 * @param pck
	 */
	public void disabledConnectionTry(ServerContext senderContext, SendToOne packet) {
		var sender = packet.getSender();
		var receiver = packet.getReceiver();
		var optionalClient = clientList.getClient(receiver);
		if(optionalClient.isEmpty()) {
			return;
		}
		var receiverContext = optionalClient.get();
		senderContext.removeRequester(receiver);
		receiverContext.removeRequester(sender);
	}



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
				System.out.println("\tKey for Client " +  "Active : " + ((ServerContext)key.attachment()).isActive() + " " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
				if(key.attachment() != null ) {
					System.out.println("\tAttachment Type: " + key.attachment().getClass());
				} else {
					System.out.println("\tNo attachment");
				}
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
					"\tClient " + "Active : " + ((ServerContext)key.attachment()).isActive() + " " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
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
