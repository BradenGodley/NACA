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



public abstract class CmdRunnable {
    public CmdArgument[] arguments;
    public abstract void run(String baseString, ArrayList<String> args);
}