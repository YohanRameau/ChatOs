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
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.chatos.context.ClientContext;
import fr.uge.chatos.context.Context;
import fr.uge.chatos.context.PrivateClientContext;
import fr.uge.chatos.core.BuildPacket;

public class Client {

	static private Logger logger = Logger.getLogger(Client.class.getName());

	private final SocketChannel sc;
	private final Selector selector;
	private final InetSocketAddress serverAddress;
	private final String login;
	private final Thread console;
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private final Map<String, PrivateClientContext> privateConnectionMap = new HashMap<>();
	private ClientContext mainContext;

	enum MessageType {
		PUBLIC_MESSAGE, PRIVATE_REQUEST, PRIVATE_MESSAGE, ACCEPT, REFUSE
	}

	public Client(String login, InetSocketAddress serverAddress) throws IOException {
		this.serverAddress = serverAddress;
		this.login = login;
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		this.console = new Thread(this::consoleRun);
	}

	//////////////////// CONSOLE THREAD ////////////////////

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
	 * @param msg
	 * @throws InterruptedException
	 */

	private void processStandardInput(String msg) throws InterruptedException {
		commandQueue.add(msg);
		selector.wakeup();
	}

	//////////////////// Main Thread ////////////////////

	/**
	 * Encode a public message into a ByteBuffer and transfer it on the server
	 * context.
	 * 
	 * @param message
	 */
	private void sendPublicMessage(String message) {

		var bb = BuildPacket.public_msg(login, message);
		mainContext.queueMessage(bb);
	}

	/**
	 * Encode a private message into a ByteBuffer and transfer it on the server
	 * context
	 * 
	 * @param message
	 */
	private void sendPrivateMessage(String input) {
		String[] tokens = input.split(" ", 2);
		if (tokens.length != 2) {
			System.out.println(
					"Parsing error: the input have a bad format. @login message for private message");
			return;
		}
		if (tokens[0].equals(login)) {
			// TODO CANNOT SEND A PRIVATE MESSAGE FOR HIMSELF.
			System.out.println("You cannot send a private message for youself");
			return;
		}
		var bb = BuildPacket.private_msg(login, tokens[0], tokens[1]);
		mainContext.queueMessage(bb);
	}

	private void sendPrivateConnexionRequest(String input) {
		String[] tokens = input.split(" ", 2);
		if (tokens.length != 1 && tokens.length != 2) {
			System.out.println(
					"Parsing error: the input have a bad format. /login message for private connexion request");
			return;
		}
		if (tokens.length == 2 && tokens[0].equals(login)) {
			System.out.println("YOU CANNOT SEND A PRIVATE request FOR YOURSELF.");
			return;
		}
		var bb = BuildPacket.request_co_private(login, tokens[0]);
		mainContext.queueMessage(bb);
	}

	private void sendPrivateConnexionAcceptance(String input) {
		String[] tokens = input.split(" ", 2);
		var bb = BuildPacket.accept_co_private(login, tokens[1]);
		mainContext.queueMessage(bb);
	}

	private void sendPrivateConnexionRefusal(String input) {
		String[] tokens = input.split(" ", 2);
		var bb = BuildPacket.refuse_co_private(login, tokens[1]);
		mainContext.queueMessage(bb);
	}

	public void initializePrivateConnection(long id) throws IOException {
		SocketChannel privateSc = SocketChannel.open();
		privateSc.configureBlocking(false);
		var key = privateSc.register(selector, SelectionKey.OP_CONNECT);
		var ctx = new PrivateClientContext(key, login + 1, this, id);
		key.attach(ctx);
		privateSc.connect(serverAddress);
	}
	
	public void registerLogin(String login, PrivateClientContext ctx){
		privateConnectionMap.put(login, ctx);
	}

	/**
	 * Parse an input to determinate the type of packet. content.
	 * 
	 * @param msg
	 */
	private MessageType parseStandardInput(String msg) {
		Objects.requireNonNull(msg);
		if (msg.length() > 0) {
			switch (msg.charAt(0)) {
			case '@':
				return MessageType.PRIVATE_MESSAGE;
			case '/':
				return MessageType.PRIVATE_REQUEST;
			case '\\':
				String[] tokens = msg.split(" ", 2);
				if (tokens.length != 2) {
					throw new IllegalStateException("Parsing error");
				}
				switch (tokens[0].toLowerCase()) {
				case "\\yes":
					return MessageType.ACCEPT;
				case "\\no":
					return MessageType.REFUSE;
				default:
					return MessageType.REFUSE;
				}
			default:
				return MessageType.PUBLIC_MESSAGE;
			}
		}
		return MessageType.PUBLIC_MESSAGE;
	}

	/**
	 * Determinate and process the command receive on the commandQueue by the
	 * Console Thread. The command can be a public message, a private message or a
	 * private connection request.
	 */
	private void processCommands() {
		if (commandQueue.isEmpty()) {
			return;
		}
		String msg = commandQueue.remove();
		switch (parseStandardInput(msg)) {
		case PUBLIC_MESSAGE:
			sendPublicMessage(msg);
			break;
		case PRIVATE_MESSAGE:
			sendPrivateMessage(msg.substring(1));
			break;
		case PRIVATE_REQUEST:
			sendPrivateConnexionRequest(msg.substring(1));
			break;
		case ACCEPT:
			sendPrivateConnexionAcceptance(msg.substring(1));
			break;
		case REFUSE:
			sendPrivateConnexionRefusal(msg.substring(1));
			break;
		}
	}

	public void launch() throws IOException {
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		mainContext = new ClientContext(key, login, this);
		key.attach(mainContext);
		sc.connect(serverAddress);
		console.start();
		while (!Thread.interrupted()) {
			// printKeys();
			try {
				selector.select(this::treatKey);
				processCommands();
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
