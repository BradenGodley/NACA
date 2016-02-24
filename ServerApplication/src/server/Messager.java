/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.util.ArrayList;

/**
 *
 * @author bg
 */
public class Messager {
    private final MainInterface vc;
    private final ArrayList<Message> messages;
    
    
    public Messager(MainInterface vc) {
        this.vc = vc;
        
        messages = new ArrayList();
    }
    
    public ArrayList<Message> getMessages() {
        return messages;
    }
    
    public void addMessage(Message msg) {
        messages.add(msg);
    }
}
