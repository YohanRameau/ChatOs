package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.packetreader.Packet.PacketBuilder;
import fr.uge.chatos.packetreader.Reader.ProcessStatus;

public class PacketReader {

	private final StringReader sr = new StringReader();
	private final ByteReader br = new ByteReader();
	private final IntReader ir = new IntReader();
	private enum State {DONE, WAITING_SENDER, WAITING_RECEIVER ,WAITING_MSG, ERROR}
    private State state = State.WAITING_SENDER;
    private PacketBuilder packetBuilder = new PacketBuilder();
	
    public ProcessStatus process(ByteBuffer bb) {
    	bb.flip();
    	// OPCODE
    	switch (br.process(bb)) {
    		case DONE:
    			var b = br.get();
    			System.out.println(b);
    		   	packetBuilder.setOpCode(b);
    		    state = State.WAITING_SENDER;
    		    switch (b) {
    		    	case 4:
    		    		getPublicMessage(bb);
    		    		break;
    		    	default:
    		    		return ProcessStatus.ERROR;
    		    }
    		    return ProcessStatus.DONE;
    		case REFILL:
    		    return ProcessStatus.REFILL;
    		case ERROR:
    		    state = State.ERROR;
    		    return ProcessStatus.ERROR;
    		default:
    			return ProcessStatus.ERROR;
    	}
    }
    
	public ProcessStatus getPrivateMessage(ByteBuffer bb){	
		if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
		
        // SENDER
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setSender(sr.get());
                state = State.WAITING_SENDER;
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
        
        // SIZE
     	switch (ir.process(bb)) {
     		case DONE:
     		   	packetBuilder.setSize(ir.get());
     		    state = State.WAITING_SENDER;
     		    break;
     		case REFILL:
     		    return ProcessStatus.REFILL;
     		case ERROR:
     		    state = State.ERROR;
     		    return ProcessStatus.ERROR;
     	}
        
        sr.reset();
        // RECEIVER
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setReceiver(sr.get());
                state = State.WAITING_RECEIVER;
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
        
        // SIZE
     	switch (ir.process(bb)) {
     		case DONE:
     			packetBuilder.setSize(ir.get());
     			state = State.WAITING_SENDER;
     		    break;
     		case REFILL:
     		    return ProcessStatus.REFILL;
     		case ERROR:
     		    state = State.ERROR;
     		    return ProcessStatus.ERROR;
     	}
        
        sr.reset();
        // MESSAGE
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
		
		// SIZE
		switch (ir.process(bb)) {
			case DONE:
			   	packetBuilder.setSize(ir.get());
			    state = State.WAITING_SENDER;
			    break;
			case REFILL:
			    return ProcessStatus.REFILL;
			case ERROR:
			    state = State.ERROR;
			    return ProcessStatus.ERROR;
		}
		
        // SENDER
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setSender(sr.get());
                state = State.WAITING_SENDER;
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
        
        // SIZE
     	switch (ir.process(bb)) {
     		case DONE:
     		   	packetBuilder.setSize(ir.get());
     		    state = State.WAITING_SENDER;
     		    break;
     		case REFILL:
     			return ProcessStatus.REFILL;
     		case ERROR:
     		    state = State.ERROR;
     		    return ProcessStatus.ERROR;
     	}
        
        sr.reset();
        // MESSAGE
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
	
	public ProcessStatus getIdentification(ByteBuffer bb){	
		if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
		
		
		// SIZE
		switch (ir.process(bb)) {
			case DONE:
			   	packetBuilder.setSize(ir.get());
			    state = State.WAITING_SENDER;
			    break;
			case REFILL:
			    return ProcessStatus.REFILL;
			case ERROR:
			    state = State.ERROR;
			    return ProcessStatus.ERROR;
		}
		
        // SENDER
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setSender(sr.get());
                state = State.WAITING_SENDER;
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
        return ProcessStatus.DONE;
	}
	
	public ProcessStatus getRequestConnexion(ByteBuffer bb){	
		if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
		
		// SIZE
		switch (ir.process(bb)) {
			case DONE:
			   	packetBuilder.setSize(ir.get());
			    state = State.WAITING_SENDER;
			    break;
			case REFILL:
			    return ProcessStatus.REFILL;
			case ERROR:
			    state = State.ERROR;
			    return ProcessStatus.ERROR;
		}
		
        // SENDER
        switch (sr.process(bb)) {
            case DONE:
            	packetBuilder.setSender(sr.get());
                state = State.WAITING_SENDER;
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
        
        // SIZE
     	switch (ir.process(bb)) {
     		case DONE:
     		  	packetBuilder.setSize(ir.get());
     		    state = State.WAITING_SENDER;
     		    break;
     		case REFILL:
     		    return ProcessStatus.REFILL;
     		case ERROR:
     		    state = State.ERROR;
     		    return ProcessStatus.ERROR;
     	}
        
        sr.reset();
        // RECEIVER
        switch (sr.process(bb)) {
        case DONE:
        	packetBuilder.setReceiver(sr.get());
            state = State.WAITING_RECEIVER;
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

	public void reset() {
		sr.reset();
		br.reset();
		ir.reset();
	}

}
