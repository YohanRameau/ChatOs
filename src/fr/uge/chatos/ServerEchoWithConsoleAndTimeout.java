package fr.uge.chatos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerEchoWithConsoleAndTimeout {

	static private class Context {

		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
		private boolean closed = false;
		private long lastActivity = System.currentTimeMillis();

		private Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
		}
		
		private boolean isInactive(int timeout) {
			return timeout < (System.currentTimeMillis() - lastActivity);
		}
		
		private void updateLastActivityTime() {
			lastActivity = System.currentTimeMillis();
		}
		
		private void closeIfInactive(int timeout) {
			if(isInactive(timeout)) {
				silentlyClose();
			}
		}
		
		/**
		 * Update the interestOps of the key looking only at values of the boolean
		 * closed and the ByteBuffer buff.
		 *
		 * The convention is that buff is in write-mode.
		 */
		private void updateInterestOps() {
			var interesOps = 0;

			if (!closed && bb.hasRemaining()) {
				interesOps = interesOps | SelectionKey.OP_READ;
			}
			if (bb.position() != 0) {
				interesOps |= SelectionKey.OP_WRITE;
			}
			if (interesOps == 0) {
				silentlyClose();
				return;
			}
			key.interestOps(interesOps);
		}

		private void silentlyClose() {
			try {
				sc.close();
			} catch (IOException e) {
				// ignore exception
			}
		}

		/**
		 * Performs the read action on sc
		 *
		 * The convention is that buff is in write-mode before calling doRead and is in
		 * write-mode after calling doRead
		 *
		 * @throws IOException
		 */
		private void doRead() throws IOException {
			
			switch (sc.read(bb)) {
			case -1:
				closed = true;
				updateInterestOps();
				break;
			default:
				updateLastActivityTime();
				updateInterestOps();
				break;
			}
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that buff is in write-mode before calling doWrite and is in
		 * write-mode after calling doWrite
		 *
		 * @throws IOException
		 */
		private void doWrite() throws IOException {
			bb.flip();
			sc.write(bb);
			bb.compact();
			updateInterestOps();
		}
	}

	static private int BUFFER_SIZE = 1_024;
	static private Logger logger = Logger.getLogger(ServerEchoWithConsoleAndTimeout.class.getName());
	private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(10);
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final int timeout;
	
	public ServerEchoWithConsoleAndTimeout(int port, int timeout) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		this.timeout = timeout;
	}
	
	enum CommandType {
		PUBLIC_MESSAGE, PRIVATE_MESSAGE, CONNEXION_REQUEST
	}
	
	CommandType commandParser( ) {
		
		if(cmd.length() < 1) {
			throw new IllegalArgumentException();
		}
		
		
		
		switch(cmd.charAt(0)) {
			case '@':
				return CommandType.PRIVATE_MESSAGE;
			case '/':
				return CommandType.CONNEXION_REQUEST;
			default:
				return CommandType.PUBLIC_MESSAGE;
		}
	}

	public void consoleRun() {
		try {
			var scan = new Scanner(System.in);
			while (scan.hasNextLine()) {
				var cmd = scan.nextLine();				
				sendCommand(cmd);
			}
		} catch (IOException e) {
			logger.info("Error during execution of the command");
		} finally {
			logger.info("Console thread stopping");
		}
	}

	
	
	private void processCommand() throws IOException {
		if (!queue.isEmpty()) {
			String command = queue.remove();
			switch (command) {
			case "info":
				var nbClient = (int) connectedClients(selector);
				logger.info("There are " + nbClient + " clients.");
				break;
			case "shutdown":
				shutdown();
				break;
			case "shutdownnow":
				logger.info("The server and their connections are closed.");
				shutdownNow();
				break;
			default:
				logger.info("Command :" + command + " does not exists.");
			}
		}
	}

	private long connectedClients(Selector selector) {
		return selector.keys().stream().filter(key -> key.attachment() != null).count();
	}

	private void shutdown() throws IOException {
		var serverSocketChannelKey = selector.keys().stream().filter(key -> key.isAcceptable()).findFirst();
		if(!serverSocketChannelKey.isEmpty()) {
			silentlyClose(serverSocketChannelKey.get());
			return;
		}
		throw new IllegalStateException("No server socket channel key on the selector.");
	}

	private void shutdownNow() throws IOException {
		shutdown();
		selector.keys().stream().filter(key -> key.attachment() != null).forEach(key -> {
			Context ctx = (Context) key.attachment();
			ctx.silentlyClose();
		});
	}

	public void sendCommand(String cmd) throws IOException {
		synchronized (selector) {
			queue.add(cmd.toLowerCase());
		}
		selector.wakeup();
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		new Thread(this::consoleRun).start();
		while (!Thread.interrupted()) {
			//printKeys(); // for debug
			//System.out.println("Starting select");
			try {
				selector.select(this::treatKey);
				processCommand();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			//System.out.println("Select finished");
		}
	}

	private void treatKey(SelectionKey key) {
		//printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		try {
			Context ctx = (Context) key.attachment();
			if(ctx != null) {
				ctx.closeIfInactive(timeout);
			}
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
		SocketChannel sc = serverSocketChannel.accept();
		if (sc != null) {
			sc.configureBlocking(false);
			SelectionKey cKey = sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			cKey.attach(new Context(cKey));
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

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 2) {
			usage();
			return;
		}
		new ServerEchoWithConsoleAndTimeout(Integer.parseInt(args[0]), Integer.parseInt(args[1])).launch();
	}

	private static void usage() {
		System.out.println("Usage : ServerEcho port timeout");
	}

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