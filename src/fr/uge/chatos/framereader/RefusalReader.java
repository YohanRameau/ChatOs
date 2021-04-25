package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.Refusal;
import fr.uge.chatos.typesreader.StringReader;

public class RefusalReader implements Reader<Refusal>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private String sender;
	
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
	 * Call actions in order to read every infos
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return ProcessStatus
	 */
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		return getSender(bb, State.DONE);
	}
	
	/**
	 * Get the Frame
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return The frame
	 */
	@Override
	public Refusal get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get but state is not Done.");
		}
		return new Refusal(sender);
	}
	
	@Override
	public void reset() {
		state = State.WAITING_SENDER;
		sr.reset();		
	}	
	
}
