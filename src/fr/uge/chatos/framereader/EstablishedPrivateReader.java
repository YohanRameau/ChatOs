package fr.uge.chatos.framereader;

import java.nio.ByteBuffer;

import fr.uge.chatos.frametypes.Established_private;

public class EstablishedPrivateReader implements Reader<Established_private>{

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		return ProcessStatus.DONE;
	}
	
	@Override
	public Established_private get() {
		return new Established_private();
	}
	
	@Override
	public void reset() {
		return;	
	}	
	
}
