/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.charset.*;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
/**
 *
 * @author bg
 */

class Command { 
    public ArrayList<String> parts;
    private String base;
    private String baseWord;
    private ArrayList<String> findSubCommands(String baseString){
        ArrayList<String> subCommands = new ArrayList();
        int start = 0;
        int end = 0;
        int cPoint = 0;
        for (char c : baseString.toCharArray()) {
            if (c==' ') {
                if (start == -1) {
                    start = cPoint;
                }else{
                    end = cPoint;
                    subCommands.add(baseString.substring(start, end));
                    start = end+1;
                }
            }
            cPoint++;
        }
        
        String lastSubCmd = baseString.substring(start);
        if (!lastSubCmd.isEmpty()) {
            subCommands.add(lastSubCmd);
        }
        return subCommands;
    }
    
    public Command(String base) {
        this.base = base.toLowerCase();
        int start = 0;
        
        for (char c : base.toCharArray()) {
            if (c == ' ') {
                start++;
            }else{
                break;
            }
        }
        
        base = base.substring(start);
        
        parts = findSubCommands(base);
        
        baseWord = parts.get(0);
    }
    
    public String getBaseWord(){
        return baseWord;
    }
}

public class ConsoleManager {
    private HashMap<String, CmdRunnable> cmds = new HashMap();
    private MainInterface mi;
    
    public void addCommand(String name, CmdRunnable cmd) {
        cmds.put(name, cmd);
    }
    
    public void removeCommand(String index) {
        cmds.remove(index);
    }
    
    public Set<String> getCommands() {
        return cmds.keySet();
    }
    
    public void enterConsole() {
        LocalTime lt = LocalTime.now();
        String cmdText = mi.cif.getText();
        mi.logEvent("> " + cmdText);
        mi.cif.setText("");
        
        
        Command cmd = new Command(cmdText);
        String baseWord = cmd.getBaseWord();
        
        
        
        if (cmd.parts.size() > 0 && cmds.get(cmd.parts.get(0)) != null) {
            if (cmds.containsKey(baseWord)) {
                ArrayList<String> args = new ArrayList();
                for (int i = 1; i < cmd.parts.size(); i++) {
                    args.add(i-1, cmd.parts.get(i));
                }
                cmds.get(baseWord).run(baseWord, args);
            }
        }else{
            mi.logEvent("Invalid command");
        }
    }
    
    public ConsoleManager(MainInterface mi) {
        this.mi = mi;
        
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enterConsole();
            }
        };
        
        mi.cta.addMouseListener(
            new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == 3){
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem clear = new JMenuItem("Clear");
                        menu.add(clear);
                        clear.addActionListener(
                                new AbstractAction() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        mi.clearConsole();
                                    }
                                }
                        );
                        JMenuItem save = new JMenuItem("Save");
                        menu.add(save);
                        save.addActionListener(
                                new AbstractAction() {
                                   @Override
                                   public void actionPerformed(ActionEvent e) {
                                        JFileChooser chooser = new JFileChooser();
                                        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                               "Text file", "txt");
                                        
                                        chooser.setFileFilter(filter);
                                        chooser.setDialogTitle("Choose where to store log files");
                                        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh mm ss");
                                        String date = sdf.format(new Date());
                                        chooser.setSelectedFile(new File(date + "console-log"));
                                        int returnVal = chooser.showOpenDialog(mi);
                                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                                            String filePath = chooser.getSelectedFile().getPath();
                                            Path file = Paths.get(filePath+".txt");
                                            
                                            if (file != null) {
                                                Charset charset = Charset.forName("US-ASCII");
                                                ////////////////MAKE THIS WORK//////////////////////////
                                                try {
                                                    String[] strs = mi.getConsoleLog().split("\n");
                                                    BufferedWriter writer = Files.newBufferedWriter(file, charset);
                                                    for (String s : strs) {
                                                        System.out.println("line: " + s);
                                                        writer.write(s);
                                                        writer.newLine();
                                                    }
                                                } catch (IOException x) {
                                                    x.printStackTrace();
                                                }
                                            }
                                        }
                                   }
                                }
                        );
                        
                        menu.show(mi.cta, e.getX(), e.getY());
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {}

                @Override
                public void mouseReleased(MouseEvent e) {}

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}

            }
        );
        
        mi.cif.addActionListener(action);
        
        // initiate console events
        
        cmds.put("echo", new CmdRunnable() {
            {
                arguments = new CmdArgument[]{new CmdArgumentTuple(false)};
            }
            @Override
            public void run(String baseCmd, ArrayList<String> parts) {
                String echoString = "";
                for (String s : parts) {
                    echoString += s + " ";
                }
                
                mi.logEvent(echoString);
            }
        });
        
        cmds.put("ping", new CmdRunnable() {
            {
                arguments = new CmdArgument[]{};
            }
            @Override
            public void run(String baseCmd, ArrayList<String> parts) {
                mi.logEvent("Pong!");
            }
        });
        
        cmds.put("ping", new CmdRunnable() {
            {
                arguments = new CmdArgument[]{};
            }
            @Override
            public void run(String baseCmd, ArrayList<String> parts) {
                mi.logEvent("Pong!");
            }
        });
        
        cmds.put("help", new CmdRunnable() {
            {
                arguments = new CmdArgument[]{};
            }
            @Override
            public void run(String baseCmd, ArrayList<String> parts) {
                mi.logEvent("Commands:");
                for (String key : cmds.keySet()) {
                    CmdRunnable kcr = cmds.get(key);
                    
                    mi.logEvent(key + " " + CmdArgument.toString(kcr.arguments));
                }
            }
        });
    }
    
    
    
}
