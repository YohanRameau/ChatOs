package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.Id_private;
import fr.uge.chatos.typesreader.LongReader;
import fr.uge.chatos.typesreader.StringReader;

public class IdPrivateReader implements Reader<Id_private>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private LongReader lr = new LongReader();
	private String sender;
	private String receiver;
	
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

	private ProcessStatus getReceiver(ByteBuffer bb, State successState) {
		if (state != State.WAITING_RECEIVER) {
			throw new IllegalStateException();
		}
		// RECEIVER
		switch (getString(bb, State.WAITING_RECEIVER, successState)) {
		case DONE:
			receiver = sr.get();
			sr.reset();
			return ProcessStatus.DONE;
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			return ProcessStatus.ERROR;
		}
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
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		System.out.println("REQUEST ID PRIVATE");
		return getIdPrivate(bb);
	}
	
	@Override
	public Id_private get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get but state not DONE.");
		}
		return new Id_private(sender, receiver, lr.get());
	}
	
	@Override
	public void reset() {
		sr.reset();	
		lr.reset();
		state = State.WAITING_SENDER;
	}	
	
}
