package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.Public_msg;
import fr.uge.chatos.typesreader.StringReader;

public class PublicMessageReader implements Reader<Public_msg>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private String sender;
	private String message;
	
//	private ProcessStatus getPublicMessage(ByteBuffer bb) {
//		if (state == State.DONE || state == State.ERROR) {
//			throw new IllegalStateException();
//		}
//		// SENDER
//		if (state == State.WAITING_SENDER) {
//			getSender(bb, State.WAITING_MSG);
//		}
//
//		// MESSAGE
//		if (state == State.WAITING_MSG) {
//			return getMessage(bb, State.DONE);
//		}
//
//		return ProcessStatus.REFILL;
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
	
	private ProcessStatus getSender(ByteBuffer bb, State successState) {
		if (state != State.WAITING_SENDER) {
			throw new IllegalStateException();
		}
		// SENDER
		switch (getString(bb, State.WAITING_SENDER, successState)) {
		case DONE:
			sender = sr.get();
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
			message = sr.get();
			sr.reset();
			return ProcessStatus.DONE;
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			return ProcessStatus.ERROR;
		}
	}
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
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

		return ProcessStatus.REFILL;
	}
	
	@Override
	public Public_msg get() {
		if (state != State.DONE) {
			throw new IllegalStateException("Get but stat is not done.");
		}
		return new Public_msg(sender, message);
	}
	
	@Override
	public void reset() {
		sr.reset();
		state = State.WAITING_SENDER;
	}	
	
}
