package org.onosproject.ovsdbrest;

/**
 * Entity capable of handling a subject connected and disconnected situation.
 */
public interface ConnectionHandler<T> {

    /**
     * Processes the connected subject.
     *
     * @param subject subject
     */
    void connected(T subject);

    /**
     * Processes the disconnected subject.
     *
     * @param subject subject.
     */
    void disconnected(T subject);
}

