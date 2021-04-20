package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.frame.Unknown_user;
import fr.uge.chatos.packetreader.FrameReader.State;

public class UnknownUserReader implements Reader<Unknown_user>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private String sender;
	
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
	
	private ProcessStatus getAnswer(ByteBuffer bb) {
		return getSender(bb, State.DONE);
	}

	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		return getAnswer(bb);
	}
	
	@Override
	public Unknown_user get() {
		return new Unknown_user();
	}
	
	@Override
	public void reset() {
		sr.reset();		
	}	
	
}