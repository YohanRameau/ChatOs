package fr.uge.chatos.packetreader;

public class Packet{
	private byte   opCode;
	private String sender;
	private String message;
	private String receiver;
	private int size;
	
	static class PacketBuilder {
		private byte   opCode;
		private String sender;
		private int size;
		private String message;
		private String receiver;
		
		public PacketBuilder() {
			
		}
		
		public PacketBuilder(byte opCode, int size, String sender) {
			this.opCode = opCode;
			this.sender = sender;
			this.size = size;
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

		public void setSize(int size) {
			this.size = size;
		}

		public void setReceiver(String receiver) {
			this.receiver = receiver;
		}

		
		public Packet build() {
			return new Packet(opCode, size, sender, message, receiver);
		}

	}
	public Packet(byte opCode, int size, String sender, String message, String receiver) {
		if(sender == null || 0 < opCode ||  5 < opCode || sender.isEmpty()  ) {
			throw new IllegalStateException();
		}
		this.opCode = opCode;
		this.sender = sender;
		this.message = message;
		this.receiver = receiver;
		this.size = size;
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
