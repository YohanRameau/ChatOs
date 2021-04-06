package fr.uge.chatos.core;

public enum PacketTypes {
	REQUEST_CO_SERVER((byte) 0),
	ACCEPTANCE((byte) 1),
	REFUSAL((byte) 2),
	REQUEST_CO_PRIVATE((byte) 3),
	PUBLIC_MSG((byte) 4),
	PRIVATE_MSG((byte) 5),
	UNKNOWN_USER((byte) 6),
	ACCEPT_CO_PRIVATE((byte) 7),
	REFUSAL_CO_PRIVATE((byte) 8),
	ID_PRIVATE((byte) 9),
	LOGIN_PRIVATE((byte) 10),
	ESTABLISHED_PRIVATE((byte) 11);
	
	public final byte opCode;
	
	private PacketTypes(byte opCode) {
		this.opCode = opCode;
	}
}
