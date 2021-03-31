package fr.uge.chatos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.packetreader.MessageReader;
import fr.uge.chatos.packetreader.Packet;
import fr.uge.chatos.packetreader.PacketReader;

public class Client {

	static private class Context {

		final private SelectionKey key;
		final private SocketChannel sc;
		final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
		final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
		final private Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode
		final private PacketReader packetReader = new PacketReader();
		private boolean closed = false;

		private Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
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
				switch (packetReader.getPublicMessage(bbin)) {
				case DONE:
					Packet pck = packetReader.getPacket();
					break;
				case REFILL:
					return;
				case ERROR:
					silentlyClose();
					return;
				}
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
			// System.out.println("WRITE");
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
			key.interestOps(SelectionKey.OP_READ);
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
		PUBLIC_MESSAGE, PRIVATE_REQUEST, PRIVATE_MESSAGE
	}

	public Client(String login, InetSocketAddress serverAddress) throws IOException {
		this.serverAddress = serverAddress;
		this.login = login;
		this.sc = SocketChannel.open();
		this.selector = Selector.open();
		this.console = new Thread(this::consoleRun);
	}

	private void consoleRun() {
		try {
			var scan = new Scanner(System.in);
			while (scan.hasNextLine()) {
				var msg = scan.nextLine();
				processStandardInput(msg);
			}
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		} finally {
			logger.info("Console thread stopping");
		}
	}

	/**
	 * Parse the standard input to determinate the type of message from the message
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
	 * Send a command to the selector via commandQueue and wake it up
	 *
	 * @param msg
	 * @throws InterruptedException
	 */

	private void processStandardInput(String msg) throws InterruptedException {
		commandQueue.add(msg);
		switch (parseStandardInput(msg)) {
		case PUBLIC_MESSAGE:
			sendPublicMessage(msg);
			break;
		case PRIVATE_MESSAGE:
			System.out.println("PRIVATE MESSAGE");
			// PRIVATE MESSAGE METHOD
			break;
		case PRIVATE_REQUEST:
			System.out.println("PRIVATE REQUEST");
			// PRIVATE REQUEST DEMAND AND GET FILE
			break;
		}
		selector.wakeup();
	}

	/**/
	private void sendPublicMessage(String message) {
		var bb = BuildPacket.public_msg(login, message);
		mainContext.queueMessage(bb);
	}

	/**
	 * Processes the command from commandQueue
	 */
	private void processCommands() {
		if (commandQueue.isEmpty()) {
			return;
		}
		String msg = commandQueue.remove();
		var loginbb = StandardCharsets.UTF_8.encode(login);
		var loginbbSize = loginbb.remaining();
		var msgbb = StandardCharsets.UTF_8.encode(msg);
		var msgbbSize = msgbb.remaining();
		int bufferSize = 2 * Integer.BYTES + loginbbSize + msgbbSize;
		if (bufferSize > 1024) {
			throw new IllegalStateException("The message is too long");
		}
		ByteBuffer bb = ByteBuffer.allocate(bufferSize);
		bb.putInt(loginbbSize).put(loginbb).putInt(msgbbSize).put(msgbb);
		bb.flip();
		mainContext.queueMessage(bb);
	}

	public void launch() throws IOException {
		sc.configureBlocking(false);
		var key = sc.register(selector, SelectionKey.OP_CONNECT);
		mainContext = new Context(key);
		key.attach(mainContext);
		sc.connect(serverAddress);

		console.start();

		while (!Thread.interrupted()) {
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

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
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
