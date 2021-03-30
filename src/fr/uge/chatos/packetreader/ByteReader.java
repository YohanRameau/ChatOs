package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

public class ByteReader implements Reader<Byte> {
    private enum State {DONE, WAITING_CODE, ERROR}
    private State state = State.WAITING_CODE;
    private static final int BUFFER_MAX_SIZE = 1024;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_MAX_SIZE);
    private Byte value;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (bb.remaining() >= Byte.BYTES) {
        	value = bb.get();
        	bb.compact();
        	state = State.DONE;
        }
        else {
        	return ProcessStatus.REFILL;
        }
        return ProcessStatus.DONE;
    }

    @Override
    public Byte get() {
    	System.out.println(state);
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
