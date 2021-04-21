package fr.uge.chatos.context;

import java.io.IOException;

import fr.uge.chatos.core.Frame;

public interface Context {
	
	void processOut();
	void processIn() throws IOException;
	void updateInterestOps();
	void doRead()  throws IOException;
	void doWrite() throws IOException;
	void silentlyClose();
	default void doConnect() throws IOException {
		return;
	}
	default public boolean privateConnection() {
		return false;
	}
	void queueMessage(Frame msg);

}
