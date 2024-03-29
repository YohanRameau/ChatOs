package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.framereader.FrameReader.State;
import fr.uge.chatos.frametypes.Request_co_server;
import fr.uge.chatos.typesreader.StringReader;

public class RequestCoServerReader implements Reader<Request_co_server>{

	private State state = State.WAITING_SENDER;
	private StringReader sr = new StringReader();
	private ClientList clientList;
	private String sender;
	
	public RequestCoServerReader(ClientList clientList) {
		this.clientList = clientList;
	}

	public RequestCoServerReader() {
		this.clientList = new ClientList();
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
		switch (sr.process(bb)) {
		case DONE:
			sender = sr.get();
			if (clientList.isPresent(sender)) {
				System.out.println("Name already taken or client already identified !");
				sr.reset();
				state = State.ERROR;
				return ProcessStatus.ERROR;
			} else {
				sr.reset();
				state = State.DONE;
				return ProcessStatus.DONE;
			}
		case REFILL:
			return ProcessStatus.REFILL;
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}
	
	/**
	 * Get the Frame
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return The frame
	 */
	@Override
	public Request_co_server get() {
		if(state != State.DONE) {
			throw new IllegalStateException("Get but state is not Done.");
		}
		return new Request_co_server(sender);
	}
	
	@Override
	public void reset() {
		state = State.WAITING_SENDER;
		sr.reset();		
	}	
	
}
