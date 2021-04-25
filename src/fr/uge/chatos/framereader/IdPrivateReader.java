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
	private long id;
	
	
	/**
	 * Read any id found on the ByteBuffer
	 * 
	 * @param bb The ByteBuffer to read on
	 * @throws IllegalStateException
	 */
	private ProcessStatus getId(ByteBuffer bb) {
		if(state != State.WAITING_ID) {
			throw new IllegalStateException();
		}
		switch(lr.process(bb)) {
		case DONE:
			state = State.DONE;
			id = lr.get();
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
	 * Read the sender found on the ByteBuffer
	 * 
	 * @param bb The ByteBuffer to read on
	 * @param successState The state the reader will be after the process
	 * @throws IllegalStateException
	 */
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

	/**
	 * Read the receiver found on the ByteBuffer
	 * 
	 * @param bb The ByteBuffer to read on
	 * @param successState The state the reader will be after the process
	 * @throws IllegalStateException
	 */
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
		
		return ProcessStatus.REFILL;
	}
	
	/**
	 * Get the Frame
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return The frame
	 */
	@Override
	public Id_private get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get but state not DONE.");
		}
		return new Id_private(sender, receiver, id);
	}
	
	@Override
	public void reset() {
		sr.reset();	
		lr.reset();
		state = State.WAITING_SENDER;
	}	
	
}
