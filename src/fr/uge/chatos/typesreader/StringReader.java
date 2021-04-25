package fr.uge.chatos.typesreader;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import fr.uge.chatos.framereader.Reader;

public class StringReader implements Reader<String> {
    private enum State {DONE, WAITING_MSG, WAITING_SIZE, ERROR}
    private State state = State.WAITING_SIZE;
    private static final int BUFFER_MAX_SIZE = 1024;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_MAX_SIZE);
    private int size;
    private String value;

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
            if (state == State.WAITING_SIZE) {
                // on vérifie s'il y a un INT
                if (bb.remaining() >= Integer.BYTES) {
                    var oldLimit = bb.limit();
                    bb.limit(Integer.BYTES - buffer.position());
                    buffer.put(bb);
                    bb.limit(oldLimit);
                    buffer.flip();
                    size = buffer.getInt();
                    if (size > BUFFER_MAX_SIZE || size < 0) {
                    	bb.compact();
                    	return ProcessStatus.ERROR;
                    }
                } else {
                    buffer.put(bb);
                    buffer.compact();
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
        

        if (size > buffer.position()){
        	bb.compact();
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        buffer.flip();
        value = StandardCharsets.UTF_8.decode(buffer).toString();
        bb.compact();
        return ProcessStatus.DONE;
    }

    /**
	 * Get the readed string
	 * 
	 * @throws IllegalStateException
	 * @return The String
	 */
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
