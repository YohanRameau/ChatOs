package fr.uge.chatos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.packetreader.Packet;
import fr.uge.chatos.packetreader.PacketReader;

public class Client {

	static private class Context {

		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
		final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
		final private Queue<ByteBuffer> queue = new LinkedList<>(); // Mettre des Packet plutot que des bytebuffer dans la queue car plusieurs types de packets.
		final private PacketReader packetReader = new PacketReader();
		final private String login;
		private Packet pck;
		private boolean closed = false;

		private Context(SelectionKey key, String login) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.login = login;
		}

		/**
		 * Process the content of bbin
		 *
		 * The convention is that bbin is in write-mode before the call to process and
		 * after the call
		 *
		 */
		private void processIn() {
			for (;;) {
				switch (packetReader.process(bbin)) {
				case DONE:
					pck = packetReader.getPacket();
					parsePacket();
					packetReader.reset();
					break;
				case REFILL:
					return;
				case ERROR:
					closed = true;
					return;
				}
			}
		}

		void parsePacket() {
			switch(pck.getOpCode()) {
            case 1:
            	System.out.println("Accepted connexion " + pck.getSender() );
            	break;
            case 2:
            	System.out.println("Refused connexion");
            	closed = true;
            	break;
            case 3:
            	System.out.println(pck.getSender() + " Sent you a private connexion request | Accept or Decline ?");
            	break;
            case 4:
            	displayMessage(pck);
            	break;
            case 5:
            	System.out.println("(Private) "+pck.getSender()+": "+pck.getMessage());
            	break;
            case 6:
            	System.out.println("The user you try to reach doesn't exist !");
            	break;
			}
			
		}
		
		void displayMessage(Packet pck) {
			if (pck.getSender().equals(login)) {
				System.out.println("Me: "+pck.getMessage());
				return;
			}
			else {
				System.out.println(pck.getSender() + ": "+pck.getMessage());
				return;
			}
		}
		
		/**
		 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
		 *
		 * @param bb
		 */
		private void queueMessage(ByteBuffer bb) {
			queue.add(bb);
			processOut();
			updateInterestOps();
		}

		/**
		 * Try to fill bbout from the message queue
		 *
		 */
		private void processOut() {
			while (!queue.isEmpty()) {
				var bb = queue.peek();
				if (bb.remaining() <= bbout.remaining()) {
					queue.remove();
					bbout.put(bb);
				} else {
					break;
				}
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
			var interesOps = 0;
			if (!closed && bbin.hasRemaining()) {
				interesOps = interesOps | SelectionKey.OP_READ;
			}
			if (bbout.position() != 0) {
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

		public void doConnect() throws IOException {
			if (!sc.finishConnect()) {
				return;
			}
			key.interestOps(SelectionKey.OP_WRITE);
			BuildPacket.request_co_server(bbout, login);
			updateInterestOps();
		}
	}

	static private int BUFFER_SIZE = 10_000;
	static private Logger logger = Logger.getLogger(Client.class.getName());

	private final SocketChannel sc;
	private final Selector selector;
	private final InetSocketAddress serverAddress;
	private final String login;
	private final Thread console;
	private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
	private Context mainContext;

	enum MessageType {
		PUBLIC_MESSAGE, PRIVATE_REQUEST, PRIVATE_MESSAGE, ACCEPT, REFUSAL
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
			}scan.close();
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
	 * Encode a public message into a ByteBuffer 
	 * and transfer it on the server context.
	 * @param message
	 */
	private void sendPublicMessage(String message) {
		var bb = BuildPacket.public_msg(login, message);
		mainContext.queueMessage(bb);
	}
	
	/**
	 * Encode a private message into a ByteBuffer 
	 * and transfer it on the server context
	 * @param message
	 */
	private void sendPrivateMessage(String input) {
		String[] tokens = input.split(" ", 2);
		if(tokens.length != 2) {
			throw new IllegalStateException("Parsing error: the input have a bad format. @login message for private message");
		}
		var bb = BuildPacket.private_msg(login, tokens[0], tokens[1]);
		mainContext.queueMessage(bb);
	}
	
	/**
	 * Parse an input to determinate the type of packet.
	 * content.
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
			default:
				return MessageType.PUBLIC_MESSAGE;
			}
		}
		return MessageType.PUBLIC_MESSAGE;
	}
	

	/**
	 * Determinate and process the command receive on the commandQueue by the Console Thread.
	 * The command can be a public message, a private message or a private connection request.
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
			sendPrivateMessage(msg);
			// PRIVATE MESSAGE METHOD
			break;
		case PRIVATE_REQUEST:
			// PRIVATE REQUEST DEMAND AND GET FILE
			break;
		}
	}

	public void launch() throws IOException {
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		mainContext = new Context(key, login);
		key.attach(mainContext);
		sc.connect(serverAddress);
		console.start();
		while (!Thread.interrupted()) {
//			printKeys();
			try {
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
		}
	}

	private void treatKey(SelectionKey key) {
		//printSelectedKey(key);
		try {
			if (key.isValid() && key.isConnectable()) {
				mainContext.doConnect();
			}
			if (key.isValid() && key.isWritable()) {
				mainContext.doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				mainContext.doRead();
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
	}
	
	private String interestOpsToString(SelectionKey key){
        if (!key.isValid()) {
            return "CANCELLED";
        }
        int interestOps = key.interestOps();
        ArrayList<String> list = new ArrayList<>();
        if ((interestOps&SelectionKey.OP_ACCEPT)!=0) list.add("OP_ACCEPT");
        if ((interestOps&SelectionKey.OP_READ)!=0) list.add("OP_READ");
        if ((interestOps&SelectionKey.OP_WRITE)!=0) list.add("OP_WRITE");
        return String.join("|",list);
    }
	
	public void printKeys() {
        Set<SelectionKey> selectionKeySet = selector.keys();
        if (selectionKeySet.isEmpty()) {
            System.out.println("The selector contains no key : this should not happen!");
            return;
        }
        System.out.println("The selector contains:");
        for (SelectionKey key : selectionKeySet){
            SelectableChannel channel = key.channel();
            if (channel instanceof ServerSocketChannel) {
                System.out.println("\tKey for ServerSocketChannel : "+ interestOpsToString(key));
            } else {
                SocketChannel sc = (SocketChannel) channel;
                System.out.println("\tKey for Client "+ remoteAddressToString(sc) +" : "+ interestOpsToString(key));
            }
        }
    }

    private String remoteAddressToString(SocketChannel sc) {
        try {
            return sc.getRemoteAddress().toString();
        } catch (IOException e){
            return "???";
        }
    }

    public void printSelectedKey(SelectionKey key) {
        SelectableChannel channel = key.channel();
        if (channel instanceof ServerSocketChannel) {
            System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
        } else {
            SocketChannel sc = (SocketChannel) channel;
            System.out.println("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
        }
    }

    private String possibleActionsToString(SelectionKey key) {
        if (!key.isValid()) {
            return "CANCELLED";
        }
        ArrayList<String> list = new ArrayList<>();
        if (key.isAcceptable()) list.add("ACCEPT");
        if (key.isReadable()) list.add("READ");
        if (key.isWritable()) list.add("WRITE");
        return String.join(" and ",list);
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
