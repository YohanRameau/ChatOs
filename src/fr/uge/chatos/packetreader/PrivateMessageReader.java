package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.frame.Private_msg;
import fr.uge.chatos.packetreader.FrameReader.State;

public class PrivateMessageReader implements Reader<Private_msg>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private String sender;
	private String receiver;
	private String message;
	
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

	}
	
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		return getPrivateMessage(bb);
	}
	
	@Override
	public Private_msg get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get call but process is not Done.");
		}
		return new Private_msg(sender, receiver, message);
	}
	
	@Override
	public void reset() {
		sr.reset();		
		state = State.WAITING_SENDER;
	}	
	
}
