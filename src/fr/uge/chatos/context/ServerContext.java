package fr.uge.chatos.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.Server;
import fr.uge.chatos.core.BuildPacket;
import fr.uge.chatos.core.LimitedQueue;
import fr.uge.chatos.packetreader.Packet;
import fr.uge.chatos.packetreader.PacketReader;

public class ServerContext implements Context{
	private static int BUFFER_SIZE = 1024;
	final private SelectionKey key;
	final private SocketChannel sc;
	final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
	final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
	final private LimitedQueue<Packet> queue = new LimitedQueue<>(20);
	final private Server server;
	private final PacketReader packetReader = new PacketReader();
	private final ClientList clientList;
	private final ArrayList<String> requesters = new ArrayList<String>();
	private boolean closed = false;
	private String login;

	public ServerContext(Server server, SelectionKey key, ClientList clientlist) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.server = server;
		this.clientList = clientlist;
	}
	
	public boolean isClient(String login) {
		return this.login.equals(login);
	}

	/**
	 * Process the identification if the client is not already connected. Send an
	 * error if the opCode is not the identification code else it send an acceptance packet.
	 * 
	 * @param opCode
	 * @param pck
	 */
	private void identificationProcess(String login) {
		if (!clientList.isPresent(login)) {
			clientList.add(login, sc);
			this.login = login;
			var acceptance_pck = new Packet.PacketBuilder((byte) 1, login).build();
			queueMessage(acceptance_pck);
			return;
		}
		var refusal_pck = new Packet.PacketBuilder((byte) 2, login).build();
		queueMessage(refusal_pck);
		closed = true;
		return;
	}
	
	/**
	 * Send an unicast to the receiver or send an unknown packet if the receiver it's not connected.
	 * @param pck
	 */
	private void unicastOrUnknow(Packet pck) {
		if (!server.unicast(pck)) {
			
			var unknown_user = new Packet.PacketBuilder((byte) 6, login).build();
			queueMessage(unknown_user);
			return;
		};
	}
	
	private void askPrivateConnection(Packet pck) {
		if(requesters.contains(pck.getReceiver())) {
			// 
			System.out.println(pck.getSender() + " already ask a private Connection " + pck.getReceiver());
			return;
		}
		addRequester(pck.getReceiver());
		if (!server.privateUnicast(pck)) {
			System.out.println(pck.getSender() + " didnt ask a private connection before " + pck.getReceiver());
			var unknown_user = new Packet.PacketBuilder((byte) 6, login).build();
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
	

	/**
	  Process an action according to the type of packet. If the client is not
	 * accepted it must to send firstly a connection request (OPCODE -> 0)
	 *  System.out.println("CLOSED");
	 * @param pck
	 */
	private void processPacket(Packet pck) {
		switch (pck.getOpCode()) {
		case 0:
			identificationProcess(pck.getSender());
		case 1:
			// TODO
			// Connection acceptance
			break;
		case 2:
			// TODO
			// Connection refusal
			break;
		case 3:
			askPrivateConnection(pck);
			//unicastOrUnknow(pck);
			//  (login 1, login 2)
			// Specific connection request
			break;
		case 4:
			server.broadcast(pck);
			// public message -> broadcast into all connected client contextqueue
			break;
		case 5:
			unicastOrUnknow(pck);
			// private message -> unicast for a specific connected client.
			break;
		case 7:			
			long id = server.generateId();
			var idPrivate1 = new Packet.PacketBuilder((byte)9, pck.getSender()).setReceiver(pck.getReceiver()).setConnectionId(id).build();
			var idPrivate2 = new Packet.PacketBuilder((byte)9, pck.getReceiver()).setReceiver(pck.getSender()).setConnectionId(id).build();
			unicastOrUnknow(idPrivate1);
			unicastOrUnknow(idPrivate2);
			break;
			//TODO
		case 8:
			if (!server.unicast(pck)) {
				var unknown_user = new Packet.PacketBuilder((byte) 6, login).build();
				queueMessage(unknown_user);
				return;
			};
			break;
			
		case 9:		
			break;
		case 10:
			var establishedPck = new Packet.PacketBuilder().setOpCode((byte) 11).build();
			queueMessage(establishedPck);
			break;
			// TODO vérification de la personne ce paquet pour ne pas usurper une connection privée.
		default:
			// TODO
		}
	}

	/**
	 * Process the content of bbin
	 *
	 * The convention is that bbin is in write-mode before the call to process and
	 * after the call
	 *
	 */
	public void processIn() {
		switch (packetReader.process(bbin)) {
		case DONE:
			processPacket(packetReader.get());
			packetReader.reset();
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
	public void queueMessage(Packet msg) {
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
			var bb = BuildPacket.encode(pck);
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
		bbout.flip();
		sc.write(bbout);
		bbout.compact();
		processOut();
		updateInterestOps();
	}
}
