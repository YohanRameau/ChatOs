package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.Login_private;
import fr.uge.chatos.typesreader.LongReader;

public class LoginPrivateReader implements Reader<Login_private>{

	private State state = State.WAITING_ID;
	private LongReader lr = new LongReader();

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
		return getId(bb) ;
	}
	
	/**
	 * Get the Frame
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return The frame
	 */
	@Override
	public Login_private get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get but state is not Done.");
		}
		return new Login_private(lr.get());
	}
	
	@Override
	public void reset() {
		state = State.WAITING_ID;
		lr.reset();		
	}	
	
}
