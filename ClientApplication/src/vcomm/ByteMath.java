/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vcomm;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 *
 * @author bg
 */
public class ByteMath {
    public static ArrayList<Byte> toArrayList(byte[] byteArray) {
        ArrayList<Byte> array = new ArrayList();
        
        for (int n = 0; n < byteArray.length; n++) {
            array.add(n, byteArray[n]);
        }
        
        return array;
    }
    
    public static byte[] toArray(ArrayList<Byte> arraylist) {
        byte[] array = new byte[arraylist.size()];
        
        for (int n = 0; n < arraylist.size(); n++) {
            array[n] = arraylist.get(n);
        }
        
        return array;
    }
    
    public static byte[] subByteArray(byte[] array, int min, int max){ 
        byte[] output = new byte[max-min];
        for (int n = min; n < max; n++) {
            output[n-min] = array[n];
        }
        return output;
    }
    
    public static byte[] subByteArray(byte[] array, int min){ 
        int max = array.length;
        byte[] output = new byte[max-min];
        for (int n = min; n < max; n++) {
            output[n-min] = array[n];
        }
        return output;
    }
    
    public static byte[] optimize(byte[] array) {
        int n = 0;
        boolean fstart = false;
        boolean fend   = false;
        int start = 0;
        int end   = array.length;
        
        for (byte b : array) { // any 0's before another number are discarded
            if (b != 0 && !fstart) {
                fstart = true;
                start = n;
            }else if(b == 0 && fstart) {
                fend = true;
                end = n;
                break;
            }
            n++;
        }
        
        return subByteArray(array, start, end);
    }
    
    public static byte[] optimize(byte[] array, int endmin) {
        int n = 0;
        boolean fstart = false;
        int start = 0;
        int end   = array.length;
        
        for (byte b : array) { // any 0's before another number are discarded
            if (b != 0 && !fstart) {
                fstart = true;
                start = n;
            }else if(b == 0 && fstart && n >= endmin) {
                System.out.println("END:" + (n+1));
                end = n+1;
                break;
            }
            System.out.println("START: " + start + " | END: " + end);
            n++;
        }
        
        return subByteArray(array, start, end);
    }
    
    public static byte[] concatByteArray(byte[] array1, byte[] array2) {
        // array1: [H][E]
        // array2: [L][L][O]
        // output: [H][E][L][L][O]
        byte[] output = new byte[array1.length+array2.length];
        int n = 0;
        for (byte b : array1) {
            output[n] = b;
            n++;
        }
        for (byte b : array2) {
            output[n] = b;
            n++;
        }
        return output;
    }
    
    public static byte[] shiftIndex(byte[] bytes, int amt) {
        // [0][0][H][E][L][L][O] len: 7
        // [E][L][L][O]       len: 4
        // shift-amt: -3
        
        byte[] output = new byte[bytes.length+amt];
        for (int n = 0; n < bytes.length; n++) {
            if (n+amt >= 0) {
                output[n+amt] = bytes[n];
            }
        }
        return output;
    }
    
    public static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
