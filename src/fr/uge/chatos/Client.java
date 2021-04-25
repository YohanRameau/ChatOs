package fr.uge.chatos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.chatos.context.ClientContext;
import fr.uge.chatos.context.Context;
import fr.uge.chatos.context.PrivateClientContext;
import fr.uge.chatos.observer.ConsoleObserver;

public class Client {

	static private final Logger logger = Logger.getLogger(Client.class.getName());
	static private final String BANNER = "\n"
			+ "───────────────────────────────────────────────────────────────────────────────────────────\n"
			+ "─██████████████─██████──██████─██████████████─██████████████─██████████████─██████████████─\n"
			+ "─██░░░░░░░░░░██─██░░██──██░░██─██░░░░░░░░░░██─██░░░░░░░░░░██─██░░░░░░░░░░██─██░░░░░░░░░░██─\n"
			+ "─██░░██████████─██░░██──██░░██─██░░██████░░██─██████░░██████─██░░██████░░██─██░░██████████─\n"
			+ "─██░░██─────────██░░██──██░░██─██░░██──██░░██─────██░░██─────██░░██──██░░██─██░░██─────────\n"
			+ "─██░░██─────────██░░██████░░██─██░░██████░░██─────██░░██─────██░░██──██░░██─██░░██████████─\n"
			+ "─██░░██─────────██░░░░░░░░░░██─██░░░░░░░░░░██─────██░░██─────██░░██──██░░██─██░░░░░░░░░░██─\n"
			+ "─██░░██─────────██░░██████░░██─██░░██████░░██─────██░░██─────██░░██──██░░██─██████████░░██─\n"
			+ "─██░░██─────────██░░██──██░░██─██░░██──██░░██─────██░░██─────██░░██──██░░██─────────██░░██─\n"
			+ "─██░░██████████─██░░██──██░░██─██░░██──██░░██─────██░░██─────██░░██████░░██─██████████░░██─\n"
			+ "─██░░░░░░░░░░██─██░░██──██░░██─██░░██──██░░██─────██░░██─────██░░░░░░░░░░██─██░░░░░░░░░░██─\n"
			+ "─██████████████─██████──██████─██████──██████─────██████─────██████████████─██████████████─\n"
			+ "───────────────────────────────────────────────────────────────────────────────────────────";
	private final SocketChannel sc;
	private final Selector selector;
	private final InetSocketAddress serverAddress;
	private final String login;
	private final Thread console;
	private final HashSet<String> requesters = new HashSet<>();
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private final Map<String, PrivateClientContext> privateConnectionMap = new HashMap<>();
	private ClientContext mainContext;


	

	public Client(String login, InetSocketAddress serverAddress) throws IOException {
		this.serverAddress = serverAddress;
		this.login = login;
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		this.console = new Thread(this::consoleRun);
	}

	//////////////////// CONSOLE THREAD ////////////////////

	/**
	 * Start the Console Thread
	 */
	
	private void consoleRun() {
		try {
			var scan = new Scanner(System.in);
			while (scan.hasNextLine()) {
				var msg = scan.nextLine();
				processStandardInput(msg);
			}
			scan.close();
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		} finally {
			logger.info("Console thread stopping");
		}
	}

	/**
	 * Send a command to the selector via commandQueue and wake it up
	 *
	 * @param msg to add to the client queue
	 * @throws InterruptedException if the msg is impossible to add
	 */

	private void processStandardInput(String msg) throws InterruptedException {
		commandQueue.add(msg);
		selector.wakeup();
	}

	//////////////////// Main Thread ////////////////////
	
	/**
	 * Add a requester to the private requester list
	 *
	 * @param login of the requester
	 * @throws IllegalStateException if clients have the same login or the connection is already established
	 */
	public void addPrivateRequester(String login) {
		if(this.login.equals(login)) {
			throw new IllegalStateException("Two clients cannot have the same login.");
		}
		if(requesters.contains(login)) {
			throw new IllegalStateException(login + " have already a private connection with you.");
		}
		requesters.add(login);
	}
	
	/**
	 * Remove a requester from the private requester list
	 *
	 * @param login of the requester
	 * @throws IllegalStateException if clients have the same login or the connection is already established
	 */
	public void removePrivateRequester(String login) {
		if(this.login.equals(login)) {
			throw new IllegalStateException("Two clients can not have the same login.");
		}
		if(!requesters.remove(login)) {
			throw new IllegalStateException("You cannot remove an inexistant requester.");
		}
	}
	
	/**
	 * Initialize a private connection
	 * 
	 * @param id received by the server
	 * @param receiver of the new connection
	 * @throws IOException in case of bad connection address
	 */
	public void initializePrivateConnection(long id, String receiver) throws IOException {
		SocketChannel privateSc = SocketChannel.open();
		privateSc.configureBlocking(false);
		var key = privateSc.register(selector, SelectionKey.OP_CONNECT);
		var ctx = new PrivateClientContext(key, receiver, this, id);
		key.attach(ctx);
		privateSc.connect(serverAddress);
		privateConnectionMap.put(receiver, ctx);
	}
	
	/**
	 * Launch the client
	 */
	public void launch() throws IOException {
		System.out.println(BANNER);
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		mainContext = new ClientContext(key, login, this);
		key.attach(mainContext);
		sc.connect(serverAddress);
		console.start();
		ConsoleObserver observer = new ConsoleObserver(this, login, requesters, mainContext, privateConnectionMap, commandQueue);
		while (!Thread.interrupted()) {
			// printKeys();
			try {
				selector.select(this::treatKey);
				observer.observe();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}

	private void treatKey(SelectionKey key) {
		try {
			if (key.isValid() && key.isConnectable()) {
				((Context) key.attachment()).doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
	}

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
	
	
	
/////////////////////////////////////////      MAIN      ////////////////////////////////////////////	

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 3) {
			usage();
			return;
		}
		new Client(args[0], new InetSocketAddress(args[1], Integer.parseInt(args[2]))).launch();
	}

	private static void usage() {
		System.out.println("Usage : ClientChat login hostname port");
	}

}