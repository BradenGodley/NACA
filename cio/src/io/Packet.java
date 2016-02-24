package io;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;

/**
 *
 * @author bg
 */
public class Packet {
    private byte protocol;
    private final ArrayList<Byte> data;
    
    private ArrayList<ArrayList<Byte>> subPackets;
    
    public byte[] getData() {
        return toArray(data);
    }
    
    public void setProtocol(byte protocol) {
        this.protocol = protocol;
    }
    
    public byte getProtocol() {
        return protocol;
    }
    
    public void addData(byte d) {
        data.add(d);
    }
    
    public ArrayList<ArrayList<Byte>> getSubPackets() {
        return subPackets;
    }
    
    public void addSubPacket(int subPacket, int subPacketPoint, byte d) {
        if (subPackets.size()-1 < subPacket) {
            subPackets.add(subPacket, new ArrayList());
        }
        subPackets.get(subPacket).add(subPacketPoint, d);
    }
    
    private ArrayList<Byte> toArrayList(byte[] byteArray) {
        ArrayList<Byte> array = new ArrayList();
        
        for (int n = 0; n < byteArray.length; n++) {
            array.add(n, byteArray[n]);
        }
        
        return array;
    }
    
    private byte[] toArray(ArrayList<Byte> arraylist) {
        byte[] array = new byte[arraylist.size()];
        
        for (int n = 0; n < arraylist.size(); n++) {
            array[n] = arraylist.get(n);
        }
        
        return array;
    }
    
    public Packet(byte protocol, byte[] data, boolean end) {
        
        subPackets = new ArrayList();
        
        this.protocol = protocol;
        this.data = toArrayList(data);
    }
    
    public Packet(byte protocol, boolean end) {
        subPackets = new ArrayList();
        
        this.protocol = protocol;
        this.data = new ArrayList();
    }
    
    public Packet() {
        subPackets = new ArrayList();
        data = new ArrayList();
        protocol = 127;
    }
}
