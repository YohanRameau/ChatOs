package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.packetreader.PacketTypes;
import fr.uge.chatos.packetreader.Packet.PacketBuilder;
import fr.uge.chatos.packetreader.Reader.ProcessStatus;

public class PacketReader {

	private final StringReader sr = new StringReader();
	private enum State {DONE, WAITING_SENDER, WAITING_RECEIVER ,WAITING_MSG, ERROR}
    private State state = State.WAITING_SENDER;
    private PacketBuilder packetBuilder;
	
    
    
	public ProcessStatus getPrivateMessage(ByteBuffer bb){	
		if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        // LOGIN
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setSender(sr.get());
                state = State.WAITING_RECEIVER;
                break;
            case REFILL:
                return ProcessStatus.REFILL;
            case ERROR:
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }

        if (state != State.WAITING_RECEIVER) {
            return ProcessStatus.ERROR;
        }
        sr.reset();
        // CONTENT
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setReceiver(sr.get());
                state = State.WAITING_MSG;
                break;
            case REFILL:
                return ProcessStatus.REFILL;
            case ERROR:
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }
        if (state != State.WAITING_MSG) {
            return ProcessStatus.ERROR;
        }
        sr.reset();
        switch (sr.process(bb)) {
        case DONE:
        	packetBuilder.setMessage(sr.get());
            state = State.WAITING_MSG;
            break;
        case REFILL:
            return ProcessStatus.REFILL;
        case ERROR:
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }
        
        return ProcessStatus.DONE;
	}
	
	
	public ProcessStatus getPublicMessage(ByteBuffer bb){	
		if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        // LOGIN
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setSender(sr.get());
                state = State.WAITING_RECEIVER;
                break;
            case REFILL:
                return ProcessStatus.REFILL;
            case ERROR:
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }

        if (state != State.WAITING_RECEIVER) {
            return ProcessStatus.ERROR;
        }
        sr.reset();
        // CONTENT
        switch (sr.process(bb)) {
        case DONE:
        	packetBuilder.setMessage(sr.get());
            state = State.WAITING_MSG;
            break;
        case REFILL:
            return ProcessStatus.REFILL;
        case ERROR:
            state = State.ERROR;
            return ProcessStatus.ERROR;
        }
        
        return ProcessStatus.DONE;
	}
	
	public Packet getPacket() {
		return packetBuilder.build();
	}
}
