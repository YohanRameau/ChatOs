package fr.uge.chatos.core;

import java.util.LinkedList;

@SuppressWarnings("serial")
public class LimitedQueue<E> extends LinkedList<E> {

	private int limit;

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    /**
	 * Attempt to add an object to the queue
	 * 
	 * @param o The object to be added
	 * @return True if the object has been added, else false
	 */
    @Override
    public boolean add(E o) {
        boolean added = super.add(o);
        while (added && size() > limit) {
           return false;
        }
        return added;
    }
}