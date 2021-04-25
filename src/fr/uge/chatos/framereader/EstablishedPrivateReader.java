package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.frametypes.Established_private;

public class EstablishedPrivateReader implements Reader<Established_private>{

	/**
	 * Call actions in order to read every infos
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return ProcessStatus
	 */
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		return ProcessStatus.DONE;
	}
	
	/**
	 * Get the Frame
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return The frame
	 */
	@Override
	public Established_private get() {
		return new Established_private();
	}
	
	@Override
	public void reset() {
		return;	
	}	
	
}
