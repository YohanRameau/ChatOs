package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.frame.Established_private;

public class EstablishedPrivateReader implements Reader<Established_private>{

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		System.out.println("REQUEST ESTABLISHED");
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
