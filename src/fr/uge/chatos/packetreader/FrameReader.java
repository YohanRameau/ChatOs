package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.ClientList;
import fr.uge.chatos.core.Frame;
import fr.uge.chatos.core.PacketTypes;

public class FrameReader implements Reader<Frame> {

	private State state = State.WAITING_BYTE;
	public Frame frame;
	private final ClientList clientList;
	private byte opCode = -1;
	
	private final ByteReader br = new ByteReader();
	private final AcceptanceReader ar = new AcceptanceReader();
	private final AcceptCoPrivateReader acpr = new AcceptCoPrivateReader();
	private final EstablishedPrivateReader epr = new EstablishedPrivateReader();
	private final IdPrivateReader ipr = new IdPrivateReader();
	private final LoginPrivateReader lpr = new LoginPrivateReader();
	private final PrivateMessageReader prmr = new PrivateMessageReader();
	private final PublicMessageReader pmr = new PublicMessageReader();
	private final RefusalCoPrivateReader recpr = new RefusalCoPrivateReader();
	private final RefusalReader rr = new RefusalReader();
	private final RequestCoPrivateReader rcprr = new RequestCoPrivateReader();
	private final UnknownUserReader uur = new UnknownUserReader();
	private final RequestCoServerReader rcsr;

	public enum State {
		DONE, WAITING_BYTE, WAITING_SENDER, WAITING_RECEIVER, WAITING_MSG, WAITING_ID, WAITING_FRAME,ERROR
	}

	public FrameReader(ClientList clientList) {
		this.clientList = clientList;
		rcsr = new RequestCoServerReader(clientList);
	}

	public FrameReader() {
		this.clientList = new ClientList();
		rcsr = new RequestCoServerReader(clientList);
	}
	
	private ProcessStatus parsePacket(ByteBuffer bb, byte b) {
		PacketTypes opcode = PacketTypes.values()[b];
		switch (opcode) {
		case REQUEST_CO_SERVER:
			return rcsr.process(bb);
		case ACCEPTANCE:
			return ar.process(bb);
		case REFUSAL:
			return rr.process(bb);
		case REQUEST_CO_PRIVATE:
			return rcprr.process(bb);
		case PUBLIC_MSG:
			return pmr.process(bb);
		case PRIVATE_MSG:
			return prmr.process(bb);
		case UNKNOWN_USER:
			return uur.process(bb);
		case ACCEPT_CO_PRIVATE:
			return acpr.process(bb);
		case REFUSAL_CO_PRIVATE:
			return recpr.process(bb);
		case ID_PRIVATE:
			return ipr.process(bb);
		case LOGIN_PRIVATE:
			return lpr.process(bb);
		case ESTABLISHED_PRIVATE:
			return epr.process(bb);
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		 if (state == State.WAITING_BYTE) {
	            switch (br.process(bb)) {
	            case DONE:
	            	System.out.println(state);
	                opCode = br.get();
	                state = State.WAITING_FRAME;
	                break;
	            case REFILL:
	                return ProcessStatus.REFILL;
	            case ERROR:
	                state = State.ERROR;
	                return ProcessStatus.ERROR;
	            default:
	                return ProcessStatus.DONE;
	            }
	        }
		 return parsePacket(bb, opCode);
	}	

	
	@Override
	public Frame get() {
		PacketTypes opcode = PacketTypes.values()[opCode];
		switch (opcode) {
		case REQUEST_CO_SERVER:
			return rcsr.get();
		case ACCEPTANCE:
			return ar.get();
		case REFUSAL:
			return rr.get();
		case REQUEST_CO_PRIVATE:
			return rcprr.get();
		case PUBLIC_MSG:
			return pmr.get();
		case PRIVATE_MSG:
			return prmr.get();
		case UNKNOWN_USER:
			return uur.get();
		case ACCEPT_CO_PRIVATE:
			return acpr.get();
		case REFUSAL_CO_PRIVATE:
			return recpr.get();
		case ID_PRIVATE:
			return ipr.get();
		case LOGIN_PRIVATE:
			return lpr.get();
		case ESTABLISHED_PRIVATE:
			return epr.get();
		default:
			return null;
		}
	}
	
	@Override
	public void reset() {
		state = State.WAITING_BYTE;
		br.reset();
		opCode = -1;
	}

}