package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.frame.Request_co_private;
import fr.uge.chatos.packetreader.FrameReader.State;

public class RequestCoPrivateReader implements Reader<Request_co_private>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private String sender;
	private String receiver;
	
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
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		System.out.println("REQUEST CO PRIVATE");
		return getRequestConnexion(bb);
	}
	
	@Override
	public Request_co_private get() {
		return new Request_co_private(sender, receiver);
	}
	
	@Override
	public void reset() {
		sr.reset();		
	}	
	
}
