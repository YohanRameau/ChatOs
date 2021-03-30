package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.packetreader.Packet.PacketBuilder;
import fr.uge.chatos.packetreader.Reader.ProcessStatus;

public class PacketReader {

	private final StringReader sr = new StringReader();
	private final ByteReader br = new ByteReader();

	private enum State {
		DONE, WAITING_BYTE, WAITING_SENDER, WAITING_RECEIVER, WAITING_MSG, ERROR
	}

	private State state = State.WAITING_BYTE;
	private PacketBuilder packetBuilder = new PacketBuilder();

	public ProcessStatus process(ByteBuffer bb) {
		System.out.println("Entree process de packet " + bb);
		// OPCODE
		try {
			if (state == State.WAITING_BYTE) {
				switch (br.process(bb)) {
				case DONE:
					var b = br.get();
					System.out.println("BYTE RECUPËREE " + bb);
					packetBuilder.setOpCode(b);
					state = State.WAITING_SENDER;
					break;
				case REFILL:
					return ProcessStatus.REFILL;
				case ERROR:
					state = State.ERROR;
					return ProcessStatus.ERROR;
				default:
					return ProcessStatus.ERROR;
				}
			}
			switch (getPublicMessage(bb)) {
			case DONE:
				System.out.println("PROCESS DONE " + bb );
				state = State.DONE;
				break;
			case REFILL:
				System.out.println("PROCESS REFILL " + bb);
				return ProcessStatus.REFILL;
			case ERROR:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			default:
				return ProcessStatus.ERROR;

			}
		} finally {
			System.out.println("Sortie process de packet " + bb);
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
			System.out.println("ENTREE GET PUBLIC MESSAGE " + bb);
			// SENDER
			switch (sr.process(bb)) {
			case DONE:
				System.out.println("SENDER DONE " + bb);
				packetBuilder.setSender(sr.get());
				sr.reset();
				state = State.WAITING_MSG;
				break;
			case REFILL:
				System.out.println("SENDER REFILL " + bb);
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
				System.out.println("MESSAGE DONE " + bb);
				packetBuilder.setMessage(sr.get());
				sr.reset();
				state = State.WAITING_MSG;
				break;
			case REFILL:
				System.out.println("MESSAGE REFILL " + bb);
				return ProcessStatus.REFILL;
			case ERROR:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
		} finally {
			System.out.println("Sortie GET PUBLIC MESSAGE " + bb);
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
		return ProcessStatus.DONE;
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
