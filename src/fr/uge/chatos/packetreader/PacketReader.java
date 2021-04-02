package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.packetreader.Packet.PacketBuilder;
import fr.uge.chatos.packetreader.Reader.ProcessStatus;

public class PacketReader {

	private final StringReader sr = new StringReader();
	private final ByteReader br = new ByteReader();
	private final ClientList clientList;

	private enum State {
		DONE, WAITING_BYTE, WAITING_SENDER, WAITING_RECEIVER, WAITING_MSG, ERROR
	}

	public PacketReader(ClientList clientList) {
		this.clientList = clientList;
	}

	public PacketReader() {
		this.clientList = new ClientList();
	}

	private State state = State.WAITING_BYTE;
	private PacketBuilder packetBuilder = new PacketBuilder();

	public ProcessStatus parsePacket(ByteBuffer bb, byte b) {
		switch (b) {
		case 0:
			return registerClient(bb);
		case 1:
			return getAnswer(bb);
		case 2:
			return getAnswer(bb);
		case 3:
			return getRequestConnexion(bb);
		case 4:
			return getPublicMessage(bb);
		case 5:
			return getPrivateMessage(bb);
		}
		return ProcessStatus.ERROR;
	}

	public ProcessStatus process(ByteBuffer bb) {
		// OPCODE
		try {
			if (state == State.WAITING_BYTE) {
				switch (br.process(bb)) {
				case DONE:
					var b = br.get();
					packetBuilder.setOpCode(b);
					return parsePacket(bb, b);
				case REFILL:
					return ProcessStatus.REFILL;
				case ERROR:
					state = State.ERROR;
					return ProcessStatus.ERROR;
				default:
					return ProcessStatus.DONE;
				}
			}
		} finally {
		}
		return ProcessStatus.DONE;
	}

	public ProcessStatus getAnswer(ByteBuffer bb) {
		System.out.println("State " + state);
		br.reset();
		// OPCODE
		switch (br.process(bb)) {
		case DONE:
			byte b = br.get();
			packetBuilder.setOpCode(b);
			br.reset();
			if(b == 1) {
				return ProcessStatus.DONE;
			}
			return ProcessStatus.ERROR;
			
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			br.reset();
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
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
	}

}