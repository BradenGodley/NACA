/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vcomm;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.*;
import io.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
/**
 *
 * @author bg
 */

class ClientInfo extends Object {
    
    private String nickname;
    private int joinTime;
    
    public ClientInfo(String nickname, int joinTime) {
        this.nickname = nickname;
        this.joinTime = joinTime;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public int getJoinTime() {
        return joinTime;
    }
    
    public void setJoinTime(int time) {
        joinTime = time;
    }
    
    public String toString() {
        return nickname;
    }
}

public class VoiceComm extends javax.swing.JFrame {
    private ArrayList<String> eventLogItems = new ArrayList<String>();
    
    private final int port = 5636;
    private static final boolean debug = true;
    private final Messager msg;
    private Socket comm;
    private final String serverIP = "localhost";
    private ClientInfo selectedClient;
    private HashMap<InetAddress, ClientInfo> clientInfoMap = new HashMap();
    
    private String nickname = "";
    
    public VoiceComm() {
        initComponents();
        logEvent("Custom VOIP System made by Braden Godley. 2/14/2016");
        
        msg = new Messager(this);
        
        clientListModel = new DefaultListModel();
        
        messagesTab.addChangeListener(
            new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    updateClientList();
                }
            }
        );
        
        
        clientList.addListSelectionListener(
            new ListSelectionListener(){
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting() == false) {
                        ClientInfo c = (ClientInfo)clientListModel.getElementAt(clientList.getSelectedIndex());
                        logEvent(c.getNickname());
                        selectedClient = c;
                        
                        updateClientInfo();
                    }
                }
            }
        );
        
        chatContentField.addActionListener(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    enterMessage();
                }
            }
        );
        
        new Thread(
            () -> {
                while (true) {
                    try {
                        updateClientInfo();
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(VoiceComm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        ).start();
        
        Runtime.getRuntime().addShutdownHook(
            new Thread( 
                () -> {
                    try {
                        IOHandler.sendRequest(comm, 126);
                        disconnect();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(VoiceComm.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(VoiceComm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            )
        );
                
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public HashMap<InetAddress, ClientInfo> getClientInfo() {
        return clientInfoMap;
    }
    
    // <editor-fold defaultstate="collapsed" desc="Basic byte manipulation stuff">
    
    
    
    
    //</editor-fold>
    
    public boolean connect(String ip) throws UnknownHostException, IOException, InterruptedException, Exception {
        setNickname(nicknameField.getText());
        InetAddress address = InetAddress.getByName(serverIP);
        Thread.sleep(2000);
        
        comm = new Socket(address, port);
        logEvent("Attempting to connect as " + getNickname());
        if (comm.isConnected()){
            connected = true;
            return true;
        }
        return false;
    }
    public void sendPacketString(byte protocol, String send) {
        if (comm.isConnected()) {
            byte[] bytes = send.getBytes(StandardCharsets.UTF_8);
            byte[] packet1 = new byte[1024];
            int usedBytes = 0;
            for (int n = 1; n < 1+bytes.length; n++){
                packet1[n] = bytes[n-1];
                usedBytes++;
            }
            usedBytes += 2;
            //   [U][S][E][R]
            //[4][U][S][E][R][-2]
            packet1[0] = protocol;
            packet1[bytes.length+1] = -2;
            
            
            
            byte[] packet = new byte[usedBytes];
            System.out.println("Sending:");
            for (int n = 0; n < usedBytes; n++) {
                packet[n] = packet1[n];
                System.out.println(packet[n]);
            }
            try {
                comm.getOutputStream().write(packet);
            } catch (IOException ex) {
                Logger.getLogger(VoiceComm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void sendNickname(){ 
        if (comm.isConnected()) {
            sendPacketString((byte)4, nickname);
        }
    }
    
    public void updateConversationBox() {
        String text = "";
        
        ArrayList<Message> msgs = msg.getMessages();
        
        for (Message m : msgs) {
            text += m.toString(this) + "\n";
        }
        
        conversationTextBox.setText(text);
    }
    
    public void listen() throws IOException, InterruptedException {
        
        InetAddress serverAddress = comm.getInetAddress();
        logEvent("Reading from " + serverAddress + ":" + port);
        
        Thread sendnn = new Thread(
            () -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(VoiceComm.class.getName()).log(Level.SEVERE, null, ex);
                }
                sendNickname();
                
                IOHandler.sendRequest(comm, 3);
            }
        );
        sendnn.start();
        
       
        
        Thread readThread = new Thread(
            () -> {
                try{
                    /*
                        1 = Print out "Woah"
                        2 = Send Message
                        3 = Sending Connected Clients' IPs
                        4 = Send client nickname
                    */
                    // read the input, and do stuff based on packets recieved.
                    main:
                    while(true){
                        Packet packet = IOHandler.readPacket(comm);
                        byte[] data = packet.getData();
                        sw:
                        switch(packet.getProtocol()){
                            case 1:
                                logEvent("Ping request recieved");
                                break;
                            case 2:
                                byte[] address = ByteMath.subByteArray(data,0,4);
                                byte[] msgBytes = ByteMath.subByteArray(data,4);
                                
                                InetAddress clientAddress = InetAddress.getByAddress(address);
                                String message = ByteMath.bytesToString(msgBytes);
                                System.out.println("Message incoming!" + message);
                                msg.addMessage(new Message(message, clientAddress));
                                
                                updateConversationBox();
                                break;
                            case 3: // 
                                for (ArrayList<Byte> sub1 : packet.getSubPackets()) {
                                    byte[] sub = ByteMath.toArray(sub1);
                                    
                                    address = ByteMath.subByteArray(sub, 0, 4);
                                    int n;
                                    for (n = 4; n < sub.length; n++) {
                                        if (sub[n] == -4){
                                            n++;
                                            break;
                                        }
                                    }
                                    byte[] timeJoined = ByteMath.subByteArray(sub, 4, n-1);
                                    byte[] nicknameBytes = ByteMath.subByteArray(sub, n);
                                            
                                    clientInfoMap.put(InetAddress.getByAddress(address), new ClientInfo(ByteMath.bytesToString(nicknameBytes), Integer.valueOf(ByteMath.bytesToString(timeJoined))));
                                }
                                
                                updateClientList();
                                break;
                            case 4: // server message broadcast
                                message = ByteMath.bytesToString(data);
                                msg.addMessage(new Message(message));
                                updateConversationBox();
                                break;
                            case 5: // update client info for an ip
                                address = ByteMath.subByteArray(data, 0, 4);
                                int n;
                                for (n = 4; n < data.length; n++) {
                                    if (data[n] == -4){
                                        n++;
                                        break;
                                    }
                                }
                                byte[] timeJoined = ByteMath.subByteArray(data, 4, n-1);
                                byte[] nicknameBytes = ByteMath.subByteArray(data, n);
                                String nick = ByteMath.bytesToString(nicknameBytes);
                                int joinTime = Integer.valueOf(ByteMath.bytesToString(timeJoined));
                                ClientInfo info = new ClientInfo(nickname, joinTime);
                                
                                getClientInfo().put(InetAddress.getByAddress(address), info);
                                
                                break;
                            case 6: // client connected/disconnected (0/1)
                                boolean joining = (data[1] == 0);
                                address = ByteMath.subByteArray(data, 1);
                                InetAddress inetAddress = InetAddress.getByAddress(address);
                                
                                if (joining) {
                                    new Thread(
                                        () -> {
                                            while (!getClientInfo().containsKey(inetAddress)) { try { Thread.sleep(1000); } catch (InterruptedException ex) { Logger.getLogger(VoiceComm.class.getName()).log(Level.SEVERE, null, ex); }
 }                                          
                                            logEvent(getClientInfo().get(inetAddress).getNickname() + " connected.");
                                            
                                            Thread.yield();
                                        }
                                    ).start();
                                }else{
                                    logEvent(getClientInfo().get(inetAddress).getNickname() + " disconnected.");
                                    
                                    getClientInfo().remove(inetAddress);
                                }
                            case 125:
                                logEvent("Nickname " + getNickname() + " already taken by another client on server. Attempt to connect with a different nickname.");
                                break main;
                            case 126:
                                logEvent("Ending connection with server " + comm.getInetAddress().toString());
                                
                                break main;
                            case 127:
                                logEvent("Empty packet...");
                                break;
                            default:
                                logEvent("Unknown protocol: " + packet.getProtocol());
                                break;
                        } 
                        
                        Thread.sleep(200);
                    }
                    disconnect();
                }catch(InterruptedException | UnknownHostException ex) {
                    if (debug)
                        Logger.getLogger(getName()).log(Level.SEVERE, null, ex);
                    logEvent("Lost connection to server: " + ex.getMessage());
                }catch(IOException ex){
                    if (debug)
                        Logger.getLogger(getName()).log(Level.SEVERE, null, ex);
                    logEvent("Lost connection to server: " + ex.getMessage());
                }
            }
        );

        readThread.start();
    }
    
    public final DefaultListModel clientListModel;
    
    public void updateClientInfo() {
        if (!clientListModel.contains("You are the only connected user.")) {
            if (selectedClient != null) {
                String nickName = selectedClient.getNickname();

                int time = (Math.round(((int)System.currentTimeMillis()-selectedClient.getJoinTime())/100)/10);

                int hours = (int) Math.ceil(time / (60*60));
                int minutes = (int) Math.ceil(time / 60) % 60;
                int seconds = (int) time % 60;
                String hourString;
                if (hours == 1) {
                    hourString = hours + " hour ";
                }else{
                    hourString = hours + " hours ";
                }
                String minuteString;
                if (minutes == 1) {
                    minuteString = minutes + " minute ";
                }else{
                    minuteString = minutes + " minutes ";
                }
                String secondString;
                if (seconds == 1) {
                    secondString = seconds + " second";
                }else{
                    secondString = seconds + " seconds";
                }
                String timestring = hourString + minuteString + secondString;
                String clientInfoText = "Nickname: " + nickName + "\n"
                        + "Total Time Since Joined: " + timestring + "\n"
                        + "";

                clientInfo.setText(clientInfoText);
            }else if (!clientInfo.getText().equals("Select a client to see info about them.")){
                clientInfo.setText("Select a client to see info about them.");
            }
        }else{
            clientInfo.setText("There are currently no connect users other than you. Wait for some people to join.");
        }
    }
    
    public void updateClientList() {
        clientListModel.clear();
        for (InetAddress address : getClientInfo().keySet()) {
            if (!address.equals(comm.getLocalAddress())) {
                ClientInfo info = getClientInfo().get(address);

                clientListModel.addElement(info);
            }
        }
        
        if (clientListModel.isEmpty())
            clientListModel.addElement("You are the only connected user.");
        
        clientList.setModel(clientListModel);
    }
    
    public void logEvent(String str){
        eventLogItems.add(str);
        System.out.println(str);
        updateTextArea();
        
        try {
            Thread.sleep(20);
        } catch (InterruptedException ex) {
            if (debug)
                ex.printStackTrace();
        }
    }
    
    public void updateTextArea(){
        ListIterator<String> list = null;
        list = eventLogItems.listIterator();
        String eventLogText = "";
        while (list.hasNext()) {
            eventLogText += list.next() + "\n";
        }
        jTextArea1.setText(eventLogText);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        ccPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jToggleButton1 = new javax.swing.JToggleButton();
        nicknameField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        messagesTab = new javax.swing.JTabbedPane();
        clientPanel = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        clientList = new javax.swing.JList<>();
        clientManager = new javax.swing.JPanel();
        requestCall = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        clientInfo = new javax.swing.JTextArea();
        chatPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        conversationTextBox = new javax.swing.JTextArea();
        chatContentField = new javax.swing.JTextField();
        chatSendButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("VOIP System");

        jSplitPane1.setDividerLocation(300);

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel1.setText("IP Address:");

        jTextField1.setText("localhost");
        jTextField1.setMargin(new java.awt.Insets(2, 4, 2, 2));
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jButton1.setText("Connect");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });

        jToggleButton1.setText("Mute");

        nicknameField.setText("User");
        nicknameField.setMargin(new java.awt.Insets(0, 4, 0, 0));

        jLabel2.setText("Nickname:");

        javax.swing.GroupLayout ccPanelLayout = new javax.swing.GroupLayout(ccPanel);
        ccPanel.setLayout(ccPanelLayout);
        ccPanelLayout.setHorizontalGroup(
            ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                    .addGroup(ccPanelLayout.createSequentialGroup()
                        .addGroup(ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nicknameField)
                            .addComponent(jTextField1)))
                    .addGroup(ccPanelLayout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToggleButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        ccPanelLayout.setVerticalGroup(
            ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ccPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nicknameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ccPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jToggleButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(ccPanel);

        jSplitPane2.setDividerLocation(150);

        jScrollPane2.setViewportView(clientList);

        jSplitPane2.setLeftComponent(jScrollPane2);

        requestCall.setText("Request Call");

        clientInfo.setColumns(20);
        clientInfo.setRows(5);
        jScrollPane3.setViewportView(clientInfo);

        javax.swing.GroupLayout clientManagerLayout = new javax.swing.GroupLayout(clientManager);
        clientManager.setLayout(clientManagerLayout);
        clientManagerLayout.setHorizontalGroup(
            clientManagerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(requestCall, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
        );
        clientManagerLayout.setVerticalGroup(
            clientManagerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(clientManagerLayout.createSequentialGroup()
                .addComponent(requestCall)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE))
        );

        jSplitPane2.setRightComponent(clientManager);

        javax.swing.GroupLayout clientPanelLayout = new javax.swing.GroupLayout(clientPanel);
        clientPanel.setLayout(clientPanelLayout);
        clientPanelLayout.setHorizontalGroup(
            clientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE)
        );
        clientPanelLayout.setVerticalGroup(
            clientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );

        messagesTab.addTab("Clients", clientPanel);

        conversationTextBox.setColumns(20);
        conversationTextBox.setRows(5);
        jScrollPane4.setViewportView(conversationTextBox);

        chatContentField.setToolTipText("Enter a message here");

        chatSendButton.setText("Send");
        chatSendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chatSendButtonMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout chatPanelLayout = new javax.swing.GroupLayout(chatPanel);
        chatPanel.setLayout(chatPanelLayout);
        chatPanelLayout.setHorizontalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE)
            .addGroup(chatPanelLayout.createSequentialGroup()
                .addComponent(chatContentField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chatSendButton))
        );
        chatPanelLayout.setVerticalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chatPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 427, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chatContentField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chatSendButton)))
        );

        messagesTab.addTab("Messages", chatPanel);

        jSplitPane1.setRightComponent(messagesTab);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void disconnect() throws IOException {
        nicknameField.setEditable(true);
        
        nicknameField.setForeground(new Color(0,0,0));
        nicknameField.setBackground(new Color(255,255,255));
        clientInfoMap.clear();
        msg.getMessages().clear();
        
        jButton1.setText("Disconnecting...");
        action = true;
        comm.close();
        action = false;
        jButton1.setText("Connect");
        
        updateClientList();
        updateClientInfo();
        updateConversationBox();
        connected = false;
    }
    
    private void connectButtonClicked() throws InterruptedException, SocketException, IOException {
        String ip = jTextField1.getText();
        
        if (ip.isEmpty()) {
            logEvent("Error: Address field empty.");
            return;
        }
        
        boolean status = false;
        
        try {
            status = connect(ip);
        } catch (Exception ex) {
            if (debug) 
                ex.printStackTrace();
            logEvent("Failed to connect to \"" + ip + ":" + String.valueOf(port) + "\" " + ex.getMessage());
        }
        if (status) {
            nicknameField.setEditable(false);
            nicknameField.setForeground(new Color(120,120,120));
            nicknameField.setBackground(new Color(220,220,220));
            logEvent("Successfully connected to \"" + ip + ":" + String.valueOf(port) + "\"");
            listen();
        }  
    }
    private boolean action = false;
    private boolean connected = false;
    @SuppressWarnings("SleepWhileInLoop")
    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseClicked
        if (!action) {
            Thread thread = new Thread(
                () -> {
                    if (comm == null || !connected) {
                        if (!nicknameField.getText().isEmpty())
                            try {
                                jButton1.setText("Connecting...");
                                action = true;
                                connectButtonClicked();
                                action = false;
                                if (comm != null && comm.isConnected()){
                                    jButton1.setText("Disconnect");
                                }else{
                                    jButton1.setText("Connect");
                                }
                            } catch (InterruptedException | SocketException ex) {
                                if (debug)
                                    ex.printStackTrace();
                            } catch (IOException ex) {
                                if (debug)
                                    ex.printStackTrace();
                            }
                    }else{
                        try {
                            //IOHandler.sendRequest(comm, 126);
                            disconnect();
                        } catch (IOException ex) {
                            if (debug)
                                ex.printStackTrace();
                        }
                    }
                }
            );

            thread.start();
        }
    }//GEN-LAST:event_jButton1MouseClicked

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    public void enterMessage() {
        if (!chatContentField.getText().isEmpty() && connected) {
            sendPacketString((byte)2, chatContentField.getText());
            chatContentField.setText("");
            
            chatContentField.requestFocus();
        }
    }
    
    private void chatSendButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_chatSendButtonMouseClicked
       enterMessage();
    }//GEN-LAST:event_chatSendButtonMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VoiceComm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VoiceComm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VoiceComm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VoiceComm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VoiceComm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ccPanel;
    private javax.swing.JTextField chatContentField;
    private javax.swing.JPanel chatPanel;
    private javax.swing.JButton chatSendButton;
    private javax.swing.JTextArea clientInfo;
    private javax.swing.JList<String> clientList;
    private javax.swing.JPanel clientManager;
    private javax.swing.JPanel clientPanel;
    private javax.swing.JTextArea conversationTextBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JTabbedPane messagesTab;
    private javax.swing.JTextField nicknameField;
    private javax.swing.JButton requestCall;
    // End of variables declaration//GEN-END:variables
}
