package fr.uge.chatos.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import fr.uge.chatos.Client;
import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.packetreader.Packet;
import fr.uge.chatos.packetreader.PacketReader;

public class ClientContext implements Context {
	
	static private int BUFFER_SIZE = 10_000;
	
	final private SelectionKey key;
	final private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Client client;
	final private Queue<ByteBuffer> queue = new LinkedList<>();
	final private PacketReader packetReader = new PacketReader();
	final private String login;
	private Packet pck;
	private boolean closed = false;

	public ClientContext(SelectionKey key, String login, Client client) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.login = login;
		this.client = client;
	}

	/**
	 * Process the content of bbin
	 *
	 * The convention is that bbin is in write-mode before the call to process and
	 * after the call
	 * 
	 * @throws IOException
	 *
	 */
	public void processIn() throws IOException {
		for (;;) {
			switch (packetReader.process(bbin)) {
			case DONE:
				pck = packetReader.get();
				parsePacket();
				packetReader.reset();
				break;
			case REFILL:
				return;
			case ERROR:
				closed = true;
				System.out.println("Error during process of packet.");
				silentlyClose();
				return;
			}
		}
	}

	void parsePacket() throws IOException {
		switch (pck.getOpCode()) {
		case 1:
			System.out.println("Accepted connexion " + pck.getSender());
			break;
		case 2:
			System.out.println("Refused connexion, try again with an other login.");
			closed = true;
			silentlyClose();
			break;
		case 3:
			System.out.println(pck.getSender()
					+ " Sent you a private connexion request | Accept (\\yes login) or Decline (\\no login) ?");
			break;
		case 4:
			displayMessage(pck);
			break;
		case 5:
			System.out.println("(Private) " + pck.getSender() + ": " + pck.getMessage());
			break;
		case 6:
			System.out.println("The user you try to reach doesn't exist !");
			break;
		case 7:
			System.out.println("User " + pck.getSender() + " has accepted the connexion request !");
			break;
		case 8:
			System.out.println("User " + pck.getSender() + " has refused the connexion request");
			break;
		case 9:
			System.out.println("User " + pck.getSender() + " and User " + pck.getReceiver()
					+ " have a private connection now with id " + pck.getConnectionId());
			client.initializePrivateConnection(pck.getConnectionId());
			break;
		
		default:
			break;
		}

	}

	public void displayMessage(Packet pck) {
		if (pck.getSender().equals(login)) {
			System.out.println("Me: " + pck.getMessage());
			return;
		} else {
			System.out.println(pck.getSender() + ": " + pck.getMessage());
			return;
		}
	}

	/**
	 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
	 *
	 * @param bb
	 */
	public void queueMessage(ByteBuffer bb) {
		queue.add(bb);
		processOut();
		updateInterestOps();
	}

	/**
	 * Try to fill bbout from the message queue
	 *
	 */
	public void processOut() {
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

	public void updateInterestOps() {
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

	public void silentlyClose() {
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
	public void doRead() throws IOException {
		if (sc.read(bbin) == -1) {
			closed = true;
			silentlyClose();
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

	public void doWrite() throws IOException {
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
		queueMessage(BuildPacket.request_co_server(login));
		updateInterestOps();
	}

}
