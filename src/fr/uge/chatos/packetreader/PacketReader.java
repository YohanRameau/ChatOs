package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.core.PacketTypes;
import fr.uge.chatos.packetreader.Packet.PacketBuilder;
import fr.uge.chatos.packetreader.Reader.ProcessStatus;

public class PacketReader implements Reader<Packet> {

	private final StringReader sr = new StringReader();
	private final ByteReader br = new ByteReader();
	private final LongReader lr = new LongReader();
	private State state = State.WAITING_BYTE;
	private PacketBuilder packetBuilder = new PacketBuilder();
	private final ClientList clientList;
	private byte opCode = -1;

	private enum State {
		DONE, WAITING_BYTE, WAITING_SENDER, WAITING_RECEIVER, WAITING_MSG, WAITING_ID , ERROR
	}

	public PacketReader(ClientList clientList) {
		this.clientList = clientList;
	}

	public PacketReader() {
		this.clientList = new ClientList();
	}

	private ProcessStatus parsePacket(ByteBuffer bb, byte b) {
		PacketTypes opcode = PacketTypes.values()[b];
		switch (opcode) {
		case REQUEST_CO_SERVER:
			return getIdentification(bb);
		case ACCEPTANCE:
			return getAnswer(bb);
		case REFUSAL:
			return getAnswer(bb);
		case REQUEST_CO_PRIVATE:
			return getRequestConnexion(bb);
		case PUBLIC_MSG:
			return getPublicMessage(bb);
		case PRIVATE_MSG:
			return getPrivateMessage(bb);
		case UNKNOWN_USER:
			return getAnswer(bb);
		case ACCEPT_CO_PRIVATE:
			return getRequestConnexion(bb);
		case REFUSAL_CO_PRIVATE:
			return getRequestConnexion(bb);
		case ID_PRIVATE:
			return getIdPrivate(bb);
		case LOGIN_PRIVATE:
			return getLoginPrivate(bb);
		case ESTABLISHED_PRIVATE:
			return ProcessStatus.DONE;
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}

	@Override
	public ProcessStatus process(ByteBuffer bb) {

		// OPCODE
		if (state == State.WAITING_BYTE) {
			switch (br.process(bb)) {
			case DONE:
				opCode = br.get();
				packetBuilder.setOpCode(opCode);
				state = State.WAITING_SENDER;
				break;
			case REFILL:
				return ProcessStatus.REFILL;
			case ERROR:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			default:
				return ProcessStatus.DONE;
			}
		}
		return parsePacket(bb, opCode);
	}

//	private State getByte(ByteBuffer bb) {
//	if (state != State.WAITING_BYTE) {
//		throw new IllegalStateException();
//	}
//	
//	
//	
//	}	

	private ProcessStatus getString(ByteBuffer bb, State waitingState, State successState) {
		if (state != waitingState) {
			throw new IllegalStateException();
		}
		switch (sr.process(bb)) {
		case DONE:
			state = successState;
			return ProcessStatus.DONE;
		case REFILL:
			state = waitingState;
			return ProcessStatus.REFILL;
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}
	
	private ProcessStatus getId(ByteBuffer bb) {
		if(state != State.WAITING_ID) {
			throw new IllegalStateException();
		}
		switch(lr.process(bb)) {
		case DONE:
			state = State.DONE;
			return ProcessStatus.DONE;
		case REFILL:
			state = State.WAITING_ID;
			return ProcessStatus.REFILL;
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}

	private ProcessStatus getSender(ByteBuffer bb, State successState) {
		if (state != State.WAITING_SENDER) {
			throw new IllegalStateException();
		}
		// SENDER
		switch (getString(bb, State.WAITING_SENDER, successState)) {
		case DONE:
			packetBuilder.setSender(sr.get());
			sr.reset();
			return ProcessStatus.DONE;
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			return ProcessStatus.ERROR;
		}
	}

	private ProcessStatus getReceiver(ByteBuffer bb, State successState) {
		if (state != State.WAITING_RECEIVER) {
			throw new IllegalStateException();
		}
		// RECEIVER
		switch (getString(bb, State.WAITING_RECEIVER, successState)) {
		case DONE:
			packetBuilder.setReceiver(sr.get());
			sr.reset();
			return ProcessStatus.DONE;
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			return ProcessStatus.ERROR;
		}
	}

	private ProcessStatus getMessage(ByteBuffer bb, State successState) {
		if (state != State.WAITING_MSG) {
			throw new IllegalStateException();
		}
		// SENDER
		switch (getString(bb, State.WAITING_MSG, successState)) {
		case DONE:
			packetBuilder.setMessage(sr.get());
			sr.reset();
			return ProcessStatus.DONE;
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			return ProcessStatus.ERROR;
		}
	}

	private ProcessStatus getAnswer(ByteBuffer bb) {
		return getSender(bb, State.DONE);
	}

	private ProcessStatus getPrivateMessage(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// SENDER
		if (state == State.WAITING_SENDER) {
			getSender(bb, State.WAITING_RECEIVER);
		}

		// RECEIVER
		if (state == State.WAITING_RECEIVER) {
			getReceiver(bb, State.WAITING_MSG);
		}

		// MESSAGE
		if (state == State.WAITING_MSG) {
			return getMessage(bb, State.DONE);
		}

		return ProcessStatus.ERROR;

//		// SENDER
//		switch (sr.process(bb)) {
//		case DONE:
//			packetBuilder.setSender(sr.get());
//			state = State.WAITING_SENDER;
//			break;
//		case REFILL:
//			return ProcessStatus.REFILL;
//		case ERROR:
//			state = State.ERROR;
//			return ProcessStatus.ERROR;
//		}
//
//		if (state != State.WAITING_RECEIVER) {
//			return ProcessStatus.ERROR;
//		}
//		sr.reset();
//		// RECEIVER
//		switch (sr.process(bb)) {
//		case DONE:
//			packetBuilder.setReceiver(sr.get());
//			state = State.WAITING_RECEIVER;
//			break;
//		case REFILL:
//			return ProcessStatus.REFILL;
//		case ERROR:
//			state = State.ERROR;
//			return ProcessStatus.ERROR;
//		}
//		if (state != State.WAITING_MSG) {
//			return ProcessStatus.ERROR;
//		}
//		sr.reset();
//		// MESSAGE
//		switch (sr.process(bb)) {
//		case DONE:
//			packetBuilder.setMessage(sr.get());
//			state = State.WAITING_MSG;
//			break;
//		case REFILL:
//			return ProcessStatus.REFILL;
//		case ERROR:
//			state = State.ERROR;
//			return ProcessStatus.ERROR;
//		}
//
//		return ProcessStatus.DONE;
	}

	private ProcessStatus getPublicMessage(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		// SENDER
		if (state == State.WAITING_SENDER) {
			getSender(bb, State.WAITING_MSG);
		}

		// MESSAGE
		if (state == State.WAITING_MSG) {
			return getMessage(bb, State.DONE);
		}

		return ProcessStatus.ERROR;

	}

	private ProcessStatus getIdentification(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// SENDER
		switch (sr.process(bb)) {
		case DONE:
			var sender = sr.get();
			if (clientList.isPresent(sender)) {
				System.out.println("Name already taken or client already identified !");
				sr.reset();
				state = State.ERROR;
				return ProcessStatus.ERROR;
			} else {
				packetBuilder.setSender(sender);
				sr.reset();
				state = State.DONE;
				return ProcessStatus.DONE;
			}
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}

	private ProcessStatus getRequestConnexion(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// SENDER
		if (state == State.WAITING_SENDER) {
			getSender(bb, State.WAITING_RECEIVER);
		}

		// RECEIVER
		if (state == State.WAITING_RECEIVER) {
			return getReceiver(bb, State.DONE);
		}
		return ProcessStatus.ERROR;

	}
	
	

	private ProcessStatus getIdPrivate(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// SENDER
		if (state == State.WAITING_SENDER) {
			getSender(bb, State.WAITING_RECEIVER);
		}

		// RECEIVER
		if (state == State.WAITING_RECEIVER) {
			getReceiver(bb, State.WAITING_ID);
		}
		
		// ID 
		if(state == State.WAITING_ID) {
			return getId(bb);
		}
		
		return ProcessStatus.ERROR;
	}
	
	private ProcessStatus getLoginPrivate(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {	
			throw new IllegalStateException();
		}
		state = State.WAITING_ID;
		return getId(bb) ;
	}
	
	@Override
	public Packet get() {
		return packetBuilder.build();
	}
	
	@Override
	public void reset() {
		state = State.WAITING_BYTE;
		sr.reset();
		br.reset();
		lr.reset();
		opCode = -1;
	}

}