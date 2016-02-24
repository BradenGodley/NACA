/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vcomm;

import java.util.ArrayList;

/**
 *
 * @author bg
 */
public class Messager {
    private final VoiceComm vc;
    private final ArrayList<Message> messages;
    
    
    public Messager(VoiceComm vc) {
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
