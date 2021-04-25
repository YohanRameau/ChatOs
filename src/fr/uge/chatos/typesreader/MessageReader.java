package fr.uge.chatos.typesreader;

import java.nio.ByteBuffer;

import fr.uge.chatos.framereader.Reader;

public class MessageReader implements Reader<MessageReader.Message> {
    private enum State {DONE, WAITING_LOGIN, WAITING_MSG, ERROR}
    private State state = State.WAITING_LOGIN;
    private Message message = new Message();
    private StringReader sr = new StringReader();

    static class Message {
        private String login;
        private String content;

        public String getLogin() {
            return login;
        }
        public String getContent() {
            return content;
        }
    }

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
        // LOGIN
        switch (sr.process(bb)) {
            case DONE:
                message.login = sr.get();
                state = State.WAITING_MSG;
                break;
            case REFILL:
                return ProcessStatus.REFILL;
            case ERROR:
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }

        if (state != State.WAITING_MSG) {
            return ProcessStatus.ERROR;
        }
        sr.reset();
        // CONTENT
        switch (sr.process(bb)) {
            case DONE:
                message.content = sr.get();
                state = State.DONE;
                break;
            case REFILL:
                return ProcessStatus.REFILL;
            case ERROR:
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }
        sr.reset();
        return ProcessStatus.DONE;
    }

    /**
	 * Get the readed message
	 * 
	 * @throws IllegalStateException
	 * @return The Message
	 */
    @Override
    public Message get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return message;
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        message = new Message();
    }
}