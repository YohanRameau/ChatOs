package fr.uge.chatos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.packetreader.Packet;
import fr.uge.chatos.packetreader.PacketReader;

public class Server {

	static private class Context {
		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
		final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
		final private Queue<Packet> queue = new LinkedList<>();
		final private Server server;
		private final PacketReader packetReader = new PacketReader();
		private final ClientList clientList;
		private boolean closed = false;
		private boolean accepted = false;
		private String login;

		private Context(Server server, SelectionKey key, ClientList clientlist) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.server = server;
			this.clientList = clientlist;
		}

		/**
		 * Process the identification if the client is not already connected. Send an
		 * error if the opCode is not the identification code else it send an acceptance packet.
		 * 
		 * @param opCode
		 * @param pck
		 */
		private void identificationProcess(String login) {
			if (!accepted && !clientList.isPresent(login)) {
				clientList.add(login, sc);
				accepted = true;
				this.login = login;
				var acceptance_pck = new Packet.PacketBuilder((byte) 1, login).build();
				queueMessage(acceptance_pck);
				return;
			}
			var refusal_pck = new Packet.PacketBuilder((byte) 2, login).build();
			queueMessage(refusal_pck);
			closed = true;
			return;
		}


		/**
		  Process an action according to the type of packet. If the client is not
		 * accepted it must to send firstly a connection request (OPCODE -> 0)
		 *  System.out.println("CLOSED");
		 * @param pck
		 */
		private void processPacket(Packet pck) {
			switch (pck.getOpCode()) {
			case 0:
				identificationProcess(pck.getSender());
			case 1:
				// TODO
				// Connection acceptance
				break;
			case 2:
				// TODO
				// Connection refusal
				break;
			case 3:
				if (!server.unicast(pck)) {
					var unknown_user = new Packet.PacketBuilder((byte) 6, login).build();
					queueMessage(unknown_user);
					return;
				};
				// Specific connection request
				break;
			case 4:
				server.broadcast(pck);
				// public message -> broadcast into all connected client contextqueue
				break;
			case 5:
				if (!server.unicast(pck)) {
					var unknown_user = new Packet.PacketBuilder((byte) 6, login).build();
					queueMessage(unknown_user);
					return;
				};
				// private message -> unicast for a specific connected client.
				break;
			case 7:
				//TODO
			case 8:
				System.out.println("Refusal packet creation");
				server.unicast(pck);
			default:
				// TODO
			}
		}

		/**
		 * Process the content of bbin
		 *
		 * The convention is that bbin is in write-mode before the call to process and
		 * after the call
		 *
		 */
		private void processIn() {
			switch (packetReader.process(bbin)) {
			case DONE:
				processPacket(packetReader.getPacket());
				packetReader.reset();
				break;
			case REFILL:
				return;
//                case RETRY:
//                	packetReader.reset();
//                	return;
			case ERROR:
				closed = true;
				if(!accepted) {
					// TODO 
					// envoyez messsage d'erreur.
				}
				return;
			}
		}

		/**
		 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
		 *
		 * @param msg
		 */
		private void queueMessage(Packet msg) {
			queue.add(msg);
			processOut();
			updateInterestOps();
		}

		/**
		 * Try to fill bbout from the message queue
		 *
		 */
		private void processOut() {

			for (;;) {
				if (queue.isEmpty()) {
					return;
				}
				var pck = queue.peek();
				var bb = BuildPacket.encode(pck);
				if (bbout.remaining() < bb.remaining()) {
					return;
				}
				bbout.put(bb);
				queue.poll();
			}
		}

		/**
		 * Update the interestOps of the key looking only at values of the boolean
		 * closed and of both ByteBuffers.
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * updateInterestOps and after the call. Also it is assumed that process has
		 * been be called just before updateInterestOps.
		 */

		private void updateInterestOps() {
			var newInterestOps = 0;
			if (!closed && bbin.hasRemaining()) {
				newInterestOps |= SelectionKey.OP_READ;
			}
			if (bbout.position() != 0) {
				newInterestOps |= SelectionKey.OP_WRITE;
			}
			if (newInterestOps == 0) {
				silentlyClose();
				return;
			}
			key.interestOps(newInterestOps);
		}

		private void silentlyClose() {
			try {
				clientList.remove(login);
				sc.close();
			} catch (IOException e) {
				// ignore exception
			}
		}

		/**
		 * Performs the read action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * doRead and after the call
		 *
		 * @throws IOException
		 */
		private void doRead() throws IOException {
			if (sc.read(bbin) == -1) {
				closed = true;
			}
			processIn();
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * doWrite and after the call
		 *
		 * @throws IOException
		 */

		private void doWrite() throws IOException {
			bbout.flip();
			sc.write(bbout);
			bbout.compact();
			processOut();
			updateInterestOps();
		}

	}

	static private final int BUFFER_SIZE = 1_024;
	static private final Logger logger = Logger.getLogger(Server.class.getName());

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final ClientList clientList = new ClientList();

	public Server(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		while (!Thread.interrupted()) {
			//printKeys(); // for debug
			try {
				selector.select(this::treatKey);
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
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
			clientKey.attach(new Context(this, clientKey, clientList));
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

	/**
	 * Add a message to all connected clients queue
	 *
	 * @param msg
	 */
	private void broadcast(Packet packet) {
		for (var key : selector.keys()) {
			var context = (Context) key.attachment();
			if (context == null) {
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
	private boolean unicast(Packet packet) {
		if (clientList.isPresent(packet.getReceiver())) {
			for (var key : selector.keys()) {
				var context = (Context) key.attachment();
				if (context == null)
					continue;
				if (context.login.equals(packet.getReceiver())) {
					context.queueMessage(packet);
					return true;
				}
			}
		}
		return false;
	}
	
	//Ajout d'une méthode unicast-like pour envoyer des réponses uniquement aux interessés

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
