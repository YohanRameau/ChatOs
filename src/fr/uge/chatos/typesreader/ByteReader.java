package fr.uge.chatos.typesreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.Reader;

public class ByteReader implements Reader<Byte> {
	private enum State {
		DONE, WAITING_CODE, ERROR
	}

	private State state = State.WAITING_CODE;
	private static final int BUFFER_MAX_SIZE = 1024;
	private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_MAX_SIZE);
	private Byte value;

	/**
	 * Call actions in order to read every infos
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return ProcessStatus
	 */
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		
		bb.flip();
		try {
			if (bb.remaining() >= Byte.BYTES) {
				value = bb.get();
				state = State.DONE;
			} else {
				return ProcessStatus.REFILL;
			}
		} finally {
			bb.compact();
		}
		return ProcessStatus.DONE;
	}

	/**
	 * Get the readed byte
	 * 
	 * @throws IllegalStateException
	 * @return The byte
	 */
	@Override
	public Byte get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return value;
	}

	@Override
	public void reset() {
		state = State.WAITING_CODE;
		buffer.clear();
		value = -1;
	}
}
