package fr.uge.chatos.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.uge.chatos.Client;
import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.ClientFrameVisitor;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.LimitedQueue;
import fr.uge.chatos.frame.Public_msg;
import fr.uge.chatos.packetreader.FrameReader;

public class ClientContext implements Context {
	
	static private int BUFFER_SIZE = 10_000;
	
	final private SelectionKey key;
	final private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private Client client;
	final private LimitedQueue<ByteBuffer> queue = new LimitedQueue<>(20);
	final private FrameReader frameReader = new FrameReader();
	final private String login;
	private Frame pck;
	private ClientFrameVisitor visitor ;
	private boolean closed = false;

	public ClientContext(SelectionKey key, String login, Client client) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.login = login;
		this.client = client;
		this.visitor = new ClientFrameVisitor(client ,this);
	}

	
	private void treatFrame(Frame frame) {
		frame.accept(visitor);
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
			switch (frameReader.process(bbin)) {
			case DONE:
				pck = frameReader.get();
				treatFrame(pck);
				frameReader.reset();
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

	public void displayMessage(Public_msg pck) {
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
			System.out.println("Error during process of packet.");
			silentlyClose();
			return;
		}
		key.interestOps(interesOps);
	}

	public void silentlyClose() {
		System.out.println("Error 404");
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