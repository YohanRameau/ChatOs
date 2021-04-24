package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.Acceptance;
import fr.uge.chatos.typesreader.StringReader;

public class AcceptanceReader implements Reader<Acceptance>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private String sender;
//	
//	private ProcessStatus getAnswer(ByteBuffer bb) {
//		return getSender(bb, State.DONE);
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

	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		return getSender(bb, State.DONE);
	}
	
	@Override
	public Acceptance get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get but state is not Done.");
		}
		return new Acceptance(sender);
	}
	
	@Override
	public void reset() {
		state = State.WAITING_SENDER;
		sr.reset();		
	}	
	
}
