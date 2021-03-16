package fr.uge.chatos;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {
    private enum State {DONE, WAITING_MSG, WAITING_SIZE, ERROR}
    private State state = State.WAITING_SIZE;
    private static final int BUFFER_MAX_SIZE = 1024;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_MAX_SIZE);
    private int size;
    private String value;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        bb.flip();
        try {
            if (state == State.WAITING_SIZE) {
                // on vérifie s'il y a un INT
                if (bb.remaining() + buffer.position() >= Integer.BYTES) {
                    var oldLimit = bb.limit();
                    bb.limit(Integer.BYTES - buffer.position());
                    buffer.put(bb);
                    bb.limit(oldLimit);
                    buffer.flip();
                    size = buffer.getInt();
                    if (size > BUFFER_MAX_SIZE || size < 0) {
                        return ProcessStatus.ERROR;
                    }
                } else {
                    buffer.put(bb);
                    return ProcessStatus.REFILL;
                }
                state = State.WAITING_MSG;
                buffer.compact();
                buffer.limit(size);
            }

            if (bb.remaining() <= buffer.remaining()){
                buffer.put(bb);
            } else {
                var oldLimit = bb.limit();
                bb.limit(bb.position() + buffer.remaining());
                buffer.put(bb);
                bb.limit(oldLimit);
            }
        } finally {
            bb.compact();
        }

        if (size > buffer.position()){
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        buffer.flip();
        value = StandardCharsets.UTF_8.decode(buffer).toString();

        return ProcessStatus.DONE;
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_SIZE;
        buffer.clear();
        size = 0;
        value = "";
    }
}
