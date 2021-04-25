package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.PrivateConnectionMessage;
import fr.uge.chatos.typesreader.LongReader;
import fr.uge.chatos.typesreader.StringReader;

public class PrivateConnectionMessageReader implements Reader<PrivateConnectionMessage> {
	
	private State state = State.WAITING_ID;
	private LongReader lr = new LongReader();
	private StringReader sr = new StringReader();
	private String message;
	private long id;
	
	/**
	 * Read any string found on the ByteBuffer
	 * 
	 * @param bb The ByteBuffer to read on
	 * @param waitingState The state the reader is on
	 * @param successState The state the reader will be after the process
	 * @throws IllegalStateException
	 */
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
	
	/**
	 * Read any message found on the ByteBuffer
	 * 
	 * @param bb The ByteBuffer to read on
	 * @param successState The state the reader will be after the process
	 * @throws IllegalStateException
	 */
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
	
	/**
	 * Read any id found on the ByteBuffer
	 * 
	 * @param bb The ByteBuffer to read on
	 * @throws IllegalStateException
	 */
	private ProcessStatus getId(ByteBuffer bb, State successState) {
		if(state != State.WAITING_ID) {
			throw new IllegalStateException();
		}
		switch(lr.process(bb)) {
		case DONE:
			state = successState;
			return ProcessStatus.DONE;
		case REFILL:
			state = State.WAITING_ID;
			return ProcessStatus.REFILL;
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}
	
	/**
	 * Call actions in order to read every infos
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return ProcessStatus
	 */
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		// ID
		if (state == State.WAITING_ID) {
			getId(bb, State.WAITING_MSG);
		}

		// MESSAGE
		if (state == State.WAITING_MSG) {
			return getMessage(bb, State.DONE);
		}

		return ProcessStatus.REFILL;
	}
	
	/**
	 * Get the Frame
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return The frame
	 */
	@Override
	public PrivateConnectionMessage get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get call but process is not Done.");
		}
		return new PrivateConnectionMessage(message, id);
	}

	@Override
	public void reset() {
		state = State.WAITING_ID;
		lr.reset();
		sr.reset();
	}

}
