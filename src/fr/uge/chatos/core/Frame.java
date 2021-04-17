package fr.uge.chatos.core;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Frame {
	
	static final Charset UTF8 = StandardCharsets.UTF_8;
	static final int MAX_NICKNAME_SIZE = 24;
	static final int MAX_MESSAGE_SIZE = 512;
	
	public void accept(FrameVisitor visitor);
	public ByteBuffer encode();
}
