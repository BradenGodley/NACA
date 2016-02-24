/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 *
 * @author bg
 */
public class Message {
    public final String text;
    public final String timeString;
    public final Date date;
    public final InetAddress sender;
    public boolean server = false;
    
    public Message(String text, InetAddress sender) {
        this.text = text;
        this.sender = sender;
        
        date = new Date();
        timeString = (new SimpleDateFormat("hh:mm:ss")).format(date);
    }
    
    public Message(String text) {
        server = true;
        this.text = text;
        sender = null;
        date = new Date();
        timeString = (new SimpleDateFormat("hh:mm:ss")).format(date);
    }
    
    public String toString(MainInterface vi) {
        if (!server) {
            return "[" + timeString + "] "
                    + vi.getClientInfo().get(sender).nickname
                    + ": "
                    + text; 
        }else{
            return "[SERVER] [" + timeString + "]: " + text;
        }
    }
}
