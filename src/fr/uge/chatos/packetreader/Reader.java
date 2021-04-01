package fr.uge.chatos.packetreader;

import java.nio.ByteBuffer;

public interface Reader<T> {
    enum ProcessStatus {DONE,REFILL,ERROR,RETRY};

    ProcessStatus process(ByteBuffer bb);

    T get();

    void reset();
}