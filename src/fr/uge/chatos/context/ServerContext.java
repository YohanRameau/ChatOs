package fr.uge.chatos.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.Server;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.LimitedQueue;
import fr.uge.chatos.framereader.FrameReader;
import fr.uge.chatos.frametypes.Acceptance;
import fr.uge.chatos.frametypes.Refusal;
import fr.uge.chatos.frametypes.SendToOne;
import fr.uge.chatos.frametypes.Unknown_user;
import fr.uge.chatos.visitor.ServerFrameVisitor;

public class ServerContext implements Context{
	private static int BUFFER_SIZE = 1024;
	final private SelectionKey key;
	final private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private LimitedQueue<Frame> queue = new LimitedQueue<>(20);
	final private Server server;
	private final FrameReader frameReader = new FrameReader();
	private final ClientList clientList;
	private final ArrayList<String> requesters = new ArrayList<String>();
	private boolean closed = false;
	private ServerFrameVisitor visitor;
	private Frame pck; 
	private String login;

	public ServerContext(Server server, SelectionKey key, ClientList clientlist) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.server = server;
		this.clientList = clientlist;
		this.visitor = new ServerFrameVisitor(server, this, key);
	}
	
	public boolean isClient(String login) {
		return this.login.equals(login);
	}
	
	public void sendPrivatePacket(Frame frame ,long id) {
		var tmp = server.getPrivateConnectionInfo(id);
		if(tmp.isEmpty()) {
			System.out.println("No private connection with it's id");
			silentlyClose();
			return;
		}
		var pci = tmp.get();
		// server.sendToOtherCLient(id, this, frame)
		// TODO Finir les méthodes de PrivateConnectionInfo
		
		
	}
	

	/**
	 * Process the identification if the client is not already connected. Send an
	 * error if the opCode is not the identification code else it send an acceptance packet.
	 * 
	 * @param opCode
	 * @param pck
	 */
	public void identificationProcess(String login) {
		if (!clientList.isPresent(login)) {
			clientList.add(login, this);
			this.login = login;
			var acceptance_pck = new Acceptance(login);
			queueMessage(acceptance_pck);
			return;
		}
		var refusal_pck = new Refusal(login);
		queueMessage(refusal_pck);
		closed = true;
		return;
	}
	
	@Override
	public boolean privateConnection() {
		return visitor.privateConnection();
	}
	
	/**
	 * Send an unicast to the receiver or send an unknown packet if the receiver it's not connected.
	 * @param pck
	 */
	public void unicastOrUnknow(SendToOne pck) {
		if (!server.unicast(pck)) {
			
			var unknown_user = new Unknown_user(pck.getSender());
			queueMessage(unknown_user);
			return;
		};
	}
	
	public void askPrivateConnection(SendToOne pck) {
		if(requesters.contains(pck.getReceiver())) {
			// 
			System.out.println(pck.getSender() + " already ask a private Connection " + pck.getReceiver());
			return;
		}
		addRequester(pck.getReceiver());
		if (!server.privateUnicast(pck)) {
			System.out.println(pck.getSender() + " didnt ask a private connection before " + pck.getReceiver());
			var unknown_user = new Unknown_user(pck.getSender());
			queueMessage(unknown_user);
			return;
		};
	}
	
	
	
	public boolean addRequester(String login) {
		return requesters.add(login);
	}
	
	public void removeRequester(String login) {
		requesters.remove(login);
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
	 */
	public void processIn() {
		switch (frameReader.process(bbin)) {
		case DONE:
			System.out.println("DONE PROCESS IN");
			pck = frameReader.get();
			System.out.println("End get");
			treatFrame(pck);
			frameReader.reset();
			break;
		case REFILL:
			return;
//            case RETRY:
//            	packetReader.reset();
//            	return;
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
		queue.add(msg);
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
			var pck = queue.peek();
			var bb = pck.encode();
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
		server.disconnect(login);
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
		System.out.println("DO WRITE SERVER CONTEXT PRIVATE");
		bbout.flip();
		sc.write(bbout);
		bbout.compact();
		processOut();
		updateInterestOps();
	}
}
