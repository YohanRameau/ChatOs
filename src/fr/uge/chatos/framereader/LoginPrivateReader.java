package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.Login_private;
import fr.uge.chatos.typesreader.LongReader;

public class LoginPrivateReader implements Reader<Login_private>{

	private State state = State.WAITING_ID;
	private LongReader lr = new LongReader();
	
	private ProcessStatus getLoginPrivate(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {	
			throw new IllegalStateException();
		}
		return getId(bb) ;
	}
	
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
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		System.out.println("REQUEST LOGIN PRIVATE ");

		return getLoginPrivate(bb);
	}
	
	@Override
	public Login_private get() {
		return new Login_private(lr.get());
	}
	
	@Override
	public void reset() {
		lr.reset();		
	}	
	
}
