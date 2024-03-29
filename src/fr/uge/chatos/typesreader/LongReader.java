package fr.uge.chatos.typesreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.Reader;

public class LongReader implements Reader<Long> {

    private enum State {DONE,WAITING,ERROR};

    private State state = State.WAITING;
    private final ByteBuffer internalbb = ByteBuffer.allocate(Long.BYTES); // write-mode
    private long value;

    /**
	 * Call actions in order to read every infos
	 * 
	 * @param bb The ByteBuffer to read on
	 * @return ProcessStatus
	 */
    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state== State.DONE || state== State.ERROR) {
            throw new IllegalStateException();
        }
        bb.flip();
        try {
            if (bb.remaining()<=internalbb.remaining()){
                internalbb.put(bb);
            } else {
                var oldLimit = bb.limit();
                bb.limit(internalbb.remaining());
                internalbb.put(bb);
                bb.limit(oldLimit);
            }
        } finally {
            bb.compact();
        }
        if (internalbb.hasRemaining()){
            return ProcessStatus.REFILL;
        }
        state=State.DONE;
        internalbb.flip();
        value=internalbb.getLong();
        return ProcessStatus.DONE;
    }

    /**
	 * Get the readed long
	 * 
	 * @throws IllegalStateException
	 * @return The Long
	 */
    @Override
    public Long get() {
        if (state!= State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        internalbb.clear();
    }
}