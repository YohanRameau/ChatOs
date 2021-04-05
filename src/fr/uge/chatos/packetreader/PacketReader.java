package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.packetreader.Packet.PacketBuilder;
import fr.uge.chatos.packetreader.Reader.ProcessStatus;

public class PacketReader {

	private final StringReader sr = new StringReader();
	private final ByteReader br = new ByteReader();
	private State state = State.WAITING_BYTE;
	private PacketBuilder packetBuilder = new PacketBuilder();
	private final ClientList clientList;
	private byte opCode = -1;

	private enum State {
		DONE, WAITING_BYTE, WAITING_SENDER, WAITING_RECEIVER, WAITING_MSG, ERROR
	}

	public PacketReader(ClientList clientList) {
		this.clientList = clientList;
	}

	public PacketReader() {
		this.clientList = new ClientList();
	}

	public ProcessStatus parsePacket(ByteBuffer bb, byte b) {
		System.out.println("Parse Packet");
		switch (b) {
		case 0:
			state = State.WAITING_SENDER;
			return registerClient(bb);
		case 1:
			state = State.WAITING_SENDER;
			return getAnswer(bb);
		case 2:
			state = State.WAITING_SENDER;
			return getAnswer(bb);
		case 3:
			state = State.WAITING_SENDER;
			return getRequestConnexion(bb);
		case 4:
			state = State.WAITING_SENDER;
			return getPublicMessage(bb);
		case 5:
			state = State.WAITING_SENDER;
			return getPrivateMessage(bb);
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}

	public ProcessStatus process(ByteBuffer bb) {
		// OPCODE

		if (state == State.WAITING_BYTE) {
			switch (br.process(bb)) {
			case DONE:
				opCode = br.get();
				System.out.println("Byte : " + opCode);
				packetBuilder.setOpCode(opCode);
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
	
	private ProcessStatus getString(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (sr.process(bb)) {
		case DONE:
			packetBuilder.setSender(sr.get());
			state = State.DONE;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}

		if (state != State.DONE) {
			return ProcessStatus.ERROR;
		}
		sr.reset();
		return ProcessStatus.DONE;
	}

	public ProcessStatus getAnswer(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		// SENDER
		switch (sr.process(bb)) {
		case DONE:
			packetBuilder.setSender(sr.get());
			state = State.DONE;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}

		if (state != State.DONE) {
			return ProcessStatus.ERROR;
		}
		return ProcessStatus.DONE;
	}

	public ProcessStatus getPrivateMessage(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// SENDER
		switch (sr.process(bb)) {
		case DONE:
			packetBuilder.setSender(sr.get());
			state = State.WAITING_SENDER;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}

		if (state != State.WAITING_RECEIVER) {
			return ProcessStatus.ERROR;
		}

		sr.reset();
		// RECEIVER
		switch (sr.process(bb)) {
		case DONE:
			packetBuilder.setReceiver(sr.get());
			state = State.WAITING_RECEIVER;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
		if (state != State.WAITING_MSG) {
			return ProcessStatus.ERROR;
		}
		sr.reset();
		// MESSAGE
		switch (sr.process(bb)) {
		case DONE:
			packetBuilder.setMessage(sr.get());
			state = State.WAITING_MSG;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}

		return ProcessStatus.DONE;
	}

	public ProcessStatus getPublicMessage(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		try {
			// SENDER
			switch (sr.process(bb)) {
			case DONE:
				packetBuilder.setSender(sr.get());
				sr.reset();
				state = State.WAITING_MSG;
				break;
			case REFILL:
				return ProcessStatus.REFILL;
			case ERROR:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}

			if (state != State.WAITING_MSG) {
				return ProcessStatus.ERROR;
			}

			// MESSAGE
			switch (sr.process(bb)) {
			case DONE:
				packetBuilder.setMessage(sr.get());
				sr.reset();
				state = State.WAITING_MSG;
				break;
			case REFILL:
				return ProcessStatus.REFILL;
			case ERROR:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
		} finally {
		}

		return ProcessStatus.DONE;
	}

	public ProcessStatus getIdentification(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// SENDER
		switch (sr.process(bb)) {
		case DONE:
			var sender = sr.get();
			if (clientList.isPresent(sender)) {
				System.out.println("Name already taken or client already identified !");
				state = State.ERROR;
				return ProcessStatus.ERROR;
			} else {
				packetBuilder.setSender(sr.get());
				state = State.DONE;
				return ProcessStatus.DONE;
			}
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}

		if (state != State.WAITING_RECEIVER) {
			return ProcessStatus.ERROR;
		}
		sr.reset();
		state = State.DONE;
		return ProcessStatus.DONE;
	}

	public ProcessStatus registerClient(ByteBuffer bb) {
		switch (getIdentification(bb)) {
		case DONE:
			state = State.DONE;
			return ProcessStatus.DONE;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		default:
			return ProcessStatus.ERROR;

		}
	}

	public ProcessStatus getRequestConnexion(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// SENDER
		switch (sr.process(bb)) {
		case DONE:
			packetBuilder.setSender(sr.get());
			state = State.WAITING_SENDER;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}

		if (state != State.WAITING_RECEIVER) {
			return ProcessStatus.ERROR;
		}
		sr.reset();
		// RECEIVER
		switch (sr.process(bb)) {
		case DONE:
			packetBuilder.setReceiver(sr.get());
			state = State.WAITING_RECEIVER;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}

		return ProcessStatus.DONE;
	}

	public Packet getPacket() {
		return packetBuilder.build();
	}

	public void reset() {
		state = State.WAITING_BYTE;
		sr.reset();
		br.reset();
		opCode = -1;
	}

}