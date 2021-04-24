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
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.chatos.context.ClientContext;
import fr.uge.chatos.context.Context;
import fr.uge.chatos.context.PrivateClientContext;
import fr.uge.chatos.frametypes.Accept_co_private;
import fr.uge.chatos.frametypes.PrivateConnectionMessage;
import fr.uge.chatos.frametypes.Private_msg;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.frametypes.Refusal_co_private;
import fr.uge.chatos.frametypes.Request_co_private;

public class Client {

	static private Logger logger = Logger.getLogger(Client.class.getName());

	private final SocketChannel sc;
	private final Selector selector;
	private final InetSocketAddress serverAddress;
	private final String login;
	private final Thread console;
	private final HashSet<String> requesters = new HashSet<>();
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private final Map<String, PrivateClientContext> privateConnectionMap = new HashMap<>();
	private ClientContext mainContext;

	enum MessageType {
		PUBLIC_MESSAGE, PRIVATE_REQUEST, PRIVATE_MESSAGE, ACCEPT, REFUSE, ERROR
	}

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
		System.out.println(requesters);
		if(this.login.equals(login)) {
			throw new IllegalStateException("Two clients can not have the same login.");
		}
		if(!requesters.remove(login)) {
			throw new IllegalStateException("You cannot remove an inexistant requester.");
		}
	}
	
	
	/**
	 * Encode a public message into a ByteBuffer and transfer it on the server
	 * context.
	 * 
	 * @param message to publicly send
	 */
	private void sendPublicMessage(String message) {
		mainContext.queueMessage(new Public_msg(login, message));
	}

	/**
	 * Encode a private message into a ByteBuffer and transfer it on the server
	 * context
	 * 
	 * @param input to parse containing login and private message
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
		mainContext.queueMessage(new Private_msg(login, tokens[0], tokens[1]));
	}

	/**
	 * Add a private connection message to the client's queue
	 * 
	 * @param login of the client
	 * @param message to send
	 * @return boolean 
	 */
	private void queuePrivateConnectionMessage(PrivateClientContext pctx, String message) {
		pctx.queueMessage(new PrivateConnectionMessage(message, pctx.getId()));
	}
	
	
	/**
	 * Send a private connection request to another user
	 * 
	 * @param input to parse containing login and private message 
	 */
	private void sendPrivateConnectionRequest(String input) {
		String[] tokens = input.split(" ", 2);
		if (tokens.length != 1 && tokens.length != 2) {
			System.out.println(
					"Parsing error: the input have a bad format. /login message for private connection request");
			return;
		}
		if(tokens[0].equals(login)) {
			System.out.println("YOU CANNOT SEND A PRIVATE request FOR YOURSELF.");
			return;
		}
		
		var pctx = privateConnectionMap.get(tokens[0]);
		
		if (pctx == null) {
			mainContext.queueMessage(new Request_co_private(login, tokens[0]));
		} 
		else if(pctx != null && tokens.length == 1) {	
			System.out.println("Private connection with " + tokens[0] + " is finish.");
			pctx.silentlyClose();
		}else {
			queuePrivateConnectionMessage(pctx, tokens[1]);
		}	
	}
	

	/**
	 * Send a positive answer to a private connection request
	 * 
	 * @param input to parse containing login and private message
	 */
	private void sendPrivateConnectionAcceptance(String input) {
		String[] tokens = input.split(" ", 2);
		if(!requesters.contains(tokens[1])) {
			System.out.println(tokens[1] + " don't ask you for a private connection.");
			return;
		}
		mainContext.queueMessage(new Accept_co_private(login, tokens[1]));
	}

	/**
	 * Send a negative answer to a private connection request
	 * 
	 * @param input to parse containing login and private message
	 */
	private void sendPrivateConnectionRefusal(String input) {
		String[] tokens = input.split(" ", 2);
		if(!requesters.contains(tokens[1])) {
			System.out.println(tokens[1] + " don't ask you for a private connection.");
			
			return;
		}
		mainContext.queueMessage(new Refusal_co_private(login, tokens[1]));
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
	 * Register client into a private connection map
	 * 
	 * @param login of the client to register
	 * @param ctx the context to add to the map
	 */
	public void registerLogin(String login, PrivateClientContext ctx){
		privateConnectionMap.put(login, ctx);
	}

	/**
	 * Parse an input to determinate the type of command
	 * 
	 * @param msg to parse
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
					return MessageType.ERROR;
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
	public void processCommands() {
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
			sendPrivateConnectionRequest(msg.substring(1));
			break;
		case ACCEPT:
			sendPrivateConnectionAcceptance(msg.substring(1));
			break;
		case REFUSE:
			sendPrivateConnectionRefusal(msg.substring(1));
			break;
		case ERROR:
			System.out.println("Unknown command");
			break;
		}
	}

	
	/**
	 * Launch the client
	 */
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