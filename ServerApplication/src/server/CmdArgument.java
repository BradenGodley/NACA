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
public class CmdArgument {
    public boolean optional;
    public String argName;
    
    
    public CmdArgument(String argName, boolean optional) {
        this.argName = argName;
        this.optional = optional;
    }
    
    public String toString() {
        if (optional) {
            return "[" + argName + "]";
        }else{
            return "<" + argName + ">";
        }
    }
    
    public static String toString(CmdArgument[] args) {
        String argsString = "";
        
        for (CmdArgument arg : args) {
            argsString += arg.toString()+" ";
        }
        
        return argsString;
    }
}