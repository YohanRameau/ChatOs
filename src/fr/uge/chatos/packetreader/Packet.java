package fr.uge.chatos.packetreader;

public class Packet{
	private byte   opCode;
	private String sender;
	private String message;
	private String receiver = null;
	
	static public class PacketBuilder {
		private byte   opCode;
		private String sender;
		private String message = null;
		private String receiver = null;
		
		public PacketBuilder() {
			
		}
		
		public PacketBuilder(byte opCode, String sender) {
			this.opCode = opCode;
			this.sender = sender;
		}
		
		public PacketBuilder setOpCode(byte opCode) {
			this.opCode = opCode;
			return this;
		}

		public PacketBuilder setSender(String sender) {
			this.sender = sender;
			return this;
		}

		public PacketBuilder setMessage(String message) {
			this.message = message;
			return this;
		}


		public PacketBuilder setReceiver(String receiver) {
			this.receiver = receiver;
			return this;
		}

		
		public Packet build() {
			return new Packet(opCode, sender, message, receiver);
		}

	}
	private Packet(byte opCode, String sender, String message, String receiver) {
		if( opCode < 0 ||  opCode > 5|| sender.isEmpty()) {
			throw new IllegalStateException();
		}
		this.opCode = opCode;
		this.sender = sender;
		this.message = message;
		this.receiver = receiver;
	}
	public byte getOpCode() {
		return opCode;
	}
	public String getSender() {
		return sender;
	}
	public String getMessage() {
		return message;
	}
	public String getReceiver() {
		return receiver;
	}

	
}
