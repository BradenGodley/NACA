/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author bg
 */
public class CmdArgumentTuple extends CmdArgument {
    public String argName;
    public boolean optional;
    
    public CmdArgumentTuple(boolean optional) {
        super("...", optional);
    }
}
