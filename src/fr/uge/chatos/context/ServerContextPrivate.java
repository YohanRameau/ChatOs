package fr.uge.chatos.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.uge.chatos.Server;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.LimitedQueue;
import fr.uge.chatos.framereader.FrameReader;
import fr.uge.chatos.visitor.ServerPrivateFrameVisitor;

public class ServerContextPrivate implements Context{
	private static int BUFFER_SIZE = 1024;
	final private SelectionKey key;
	final private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private LimitedQueue<ByteBuffer> queue = new LimitedQueue<>(20);
	@SuppressWarnings("unused")
	final private Server server;
	private Frame pck;
	private ServerPrivateFrameVisitor visitor;
	private final FrameReader frameReader = new FrameReader();
	private boolean closed = false; 

	public ServerContextPrivate(Server server, SelectionKey key) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.server = server;
		this.visitor = new ServerPrivateFrameVisitor(server, this);
	}
	
	/**
	 * Check if a client has established a private connection
	 *
	 * @return True if the client is connected, else false
	 */
	@Override
	public boolean privateConnection() {
		return true;
	}
	
	/**
	 * Call the frame's specific visitor to apply actions
	 * 
	 * @param frame The frame to be treated
	 *
	 */
	private void treatFrame(Frame frame) {
		frame.accept(visitor);
	}

	/**
	 * Process the content of bbin
	 *
	 * The convention is that bbin is in write-mode before the call to process and
	 * after the call
	 *
	 */
	public void processIn() {
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
			return;
		}
	}

	/**
	 * Add a message to the message queue, tries to fill bbOut and updateInterestOps
	 *
	 * @param msg
	 */
	public void queueMessage(Frame msg) {
		queue.add(msg.encode());
		processOut();
		updateInterestOps();
	}

	/**
	 * Try to fill bbout from the message queue
	 *
	 */
	public void processOut() {

		for (;;) {
			if (queue.isEmpty()) {
				return;
			}
			var bb = queue.peek();
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

	public void updateInterestOps() {
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
	@Override
	public void doRead() throws IOException {
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
	@Override
	public void doWrite() throws IOException {
		bbout.flip();
		sc.write(bbout);
		bbout.compact();
		processOut();
		updateInterestOps();
	}
}
