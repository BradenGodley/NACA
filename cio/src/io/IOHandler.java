package io;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 *
 * @author bg
 */
public class IOHandler {
    public static boolean sendPacket(Socket client, byte[] data, int protocol) throws IOException {
        byte[] packet = new byte[data.length+2];

        packet[0] = (byte)protocol;
        packet[packet.length-1] = (byte)-2;

        System.arraycopy(data, 0, packet, 1, data.length);

        client.getOutputStream().write(packet);
        return true;
    }
    
    public static boolean sendRequest(Socket client, int protocol) {
        
        try {
            byte[] packet = {(byte)protocol, -2};
            client.getOutputStream().write(packet);
            return true;
        }catch(IOException e) {
            Logger.getLogger("IOHandler").log(Level.SEVERE, null, e);
        }
        return false;
    }
    
    public static Packet readPacket(Socket client) throws IOException {
        Packet packet = new Packet();
        int bv = 0;
        int bn = 0;

        int subPacketNum = 0;
        int subPacketPtNum = 0;
        read:
        while (true) {
            bv = client.getInputStream().read();

            if (bv != -1) {
                
                if (bv > 127)
                    bv = -(256-bv);
                System.out.println(bv);
                if (packet.getProtocol() != 127) {
                    sw:
                    switch (bv) {
                        case -2:

                            break read;
                        case -3:
                            subPacketPtNum = 0;
                            subPacketNum++;
                            break sw;
                        default:
                            packet.addData((byte)bv);
                            packet.addSubPacket(subPacketNum, subPacketPtNum, (byte)bv);
                            bn++;
                            subPacketPtNum++;
                            break sw;
                    }
                }else if (bv > 0) {
                    packet.setProtocol((byte)bv);
                }
            }else{

            }
        }

        return packet;
    }
}
