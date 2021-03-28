package fr.uge.chatos.packetreader;

public class Packet{
	private byte   opCode;
	private String sender;
	private String message;
	private String receiver;
	
	static class PacketBuilder {
		private byte   opCode;
		private String sender;
		private String message;
		private String receiver;
		
		public PacketBuilder(byte opCode, String sender) {
			if(sender == null || 0 < opCode ||  5 < opCode || sender.isEmpty()  ) {
				throw new IllegalStateException();
			}
			this.opCode = opCode;
			this.sender = sender;
		}
		
		public void setOpCode(byte opCode) {
			this.opCode = opCode;
		}

		public void setSender(String sender) {
			this.sender = sender;
		}

		public void setMessage(String message) {
			this.message = message;
		}


		public void setReceiver(String receiver) {
			this.receiver = receiver;
		}

		
		public Packet build() {
			return new Packet(opCode, sender, message, receiver);
		}

	}
	public Packet(byte opCode, String sender, String message, String receiver) {
		this.opCode = opCode;
		this.sender = sender;
		this.message = message;
		this.receiver = receiver;
	}
}
