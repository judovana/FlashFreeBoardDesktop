/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.comm;

/**
 *
 * @author jvanek
 */
public class ConnectionID {

    /**
     * id to use to be used to communicate
     */
    private final String id;
    /**
     * human nice name for listing
     */
    private final String name;

    public ConnectionID(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " [" + id + "]";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
