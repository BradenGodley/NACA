/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import io.*;
import java.awt.event.ActionEvent;
/**
 *
 * @author bg
 */
class ClientInfo {
    public String nickname;
    public InetAddress address;
    public int joinTime;
    public Socket socket;
    
    public ClientInfo(Socket client, String nickname) {
        this.nickname = nickname;
        address = client.getInetAddress();
        socket = client;
        joinTime = (int)System.currentTimeMillis();
    }
    
    public ClientInfo(Socket client) {
        nickname = null;
        address = client.getInetAddress();
        socket = client;
        joinTime = (int)System.currentTimeMillis();
    }
}

public final class MainInterface extends javax.swing.JFrame {
    private ServerSocket ServerSocket;
    public final int port = 5636;
    private String consoleLog = "";
    private final ArrayList<Socket> clientList = new ArrayList();
    private final HashMap<InetAddress, ClientInfo> cInfo;
    private Socket selectedClient = null;
    private final int maxClients = 50;
    public static final boolean debug = true;
    private boolean online = false;
    
    public Messager msg;
    
    public ConsoleManager cm;
    
    private final DefaultListModel dlm;
    /**
     * Creates new form MainInterface
     */
    public MainInterface() {
        msg = new Messager(this);
        this.dlm = new DefaultListModel();
        this.cInfo = new HashMap();
        initComponents();
        
        cif = this.consoleInputField;
        cta = this.consoleTextArea;
        
        cm = new ConsoleManager(this);
        initializeCommands();
        
        /*     
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(MainInterface.class.getName()).log(Level.SEVERE, null, ex);
        } */
        clientInfoUpdateLoop().start();
        
        logEvent("Server - console/connection management made by Braden Godley.");
        logEvent("Type 'help' to see a list of commands with their arguments");
        
        messageTextField.addActionListener(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    enterMessage();
                }
            }
        );
        
        sendMessage.addActionListener(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e){ 
                    enterMessage();
                }
            }
        );
        
    }
    
    public String getNickname(Socket client) {
        return cInfo.get(client.getInetAddress()).nickname;
    }
    
    public String getNickname(InetAddress address) {
        return cInfo.get(address).nickname;
    }
    
    public int getTimeJoined(Socket client) {
        return cInfo.get(client.getInetAddress()).joinTime;
    }
    
    public int getTimeJoined(InetAddress address) {
        return cInfo.get(address).joinTime;
    }
    
    //<editor-fold defaultstate="collapsed" desc="Write - Read - Sleep">
    public void write(OutputStream os, byte[] b) {
        try{
            os.write(b);
        }catch(IOException e ){
            Logger.getLogger(this.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public int read(InputStream is) {
        try{
            return is.read();
        }catch(IOException e) {
            
            Logger.getLogger(this.getName()).log(Level.SEVERE, null, e);
        }
        return 0;
    }
    
    public void sendPacketString(Socket comm, byte protocol, String send) {
        if (comm.isConnected()) {
            byte[] bytes = send.getBytes(StandardCharsets.UTF_8);
            byte[] packet1 = new byte[256];
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
            System.arraycopy(packet1, 0, packet, 0, usedBytes);
            try {
                comm.getOutputStream().write(packet);
            } catch (IOException ex) {
                Logger.getLogger(this.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void sendPacketString(Socket comm, byte protocol, byte[] before, String send) {
        if (comm.isConnected()) {
            byte[] bytes = send.getBytes(StandardCharsets.UTF_8);
            byte[] packet1 = new byte[256];
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
            System.arraycopy(packet1, 0, packet, 0, usedBytes);
            try {
                comm.getOutputStream().write(packet);
            } catch (IOException ex) {
                Logger.getLogger(this.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
        }catch(InterruptedException e){
            Logger.getLogger(this.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    //</editor-fold>
    
    // update client info box
    public Thread clientInfoUpdateLoop(){ 
        return new Thread(
                () ->{
                    while (true) {
                        updateClientInfo();
                        sleep(3000);
                    }
                }
        );
    }
    
    // Commands
    public void initializeCommands() {
        cm.addCommand("disconnect", 
            new CmdRunnable() {
                {
                    arguments = new CmdArgument[]{
                        new CmdArgument("client ip", false)
                    };
                }
                @Override
                public void run(String base, ArrayList<String> args) {
                    boolean found = false;
                    logEvent("Attempting disconnect of \"" + args.get(0) + "\"");
                    for (Socket client : clientList) {
                        if (client.getInetAddress().toString().equals(args.get(0))) {
                            closeConnect(client, true);
                            found = true;
                        }
                    }
                    if (found) {
                        logEvent("Disconnected \"" + args.get(0) + "\"");
                    }else{
                        logEvent("Could not find client with ip \"" + args.get(0) + "\"");
                    }
                }
            }
        );
        
        cm.addCommand("clients",
            new CmdRunnable() {
                {
                    arguments = new CmdArgument[]{};
                }
                @Override
                public void run(String base, ArrayList<String> args) {
                    logEvent("Clients (IP):");
                    for (Socket client : clientList) {
                        logEvent(cInfo.get(client.getInetAddress()).nickname + " (" + client.getInetAddress() + ") - Connected: " + client.isConnected());
                    }
                }
            }
        );
        
        cm.addCommand("clear", 
            new CmdRunnable() {
                {
                    arguments = new CmdArgument[]{};
                }
                @Override
                public void run(String base, ArrayList<String> args) {
                    consoleLog = "";
                    updateEventLog();
                }
            }
        );
        
        cm.addCommand("exit", 
            new CmdRunnable() {
                {
                    arguments = new CmdArgument[]{};
                }
                @Override
                public void run(String base, ArrayList<String> args) {
                    System.exit(0);
                }
            }
        );
        
        cm.addCommand("sendbytes", 
            new CmdRunnable() {
                {
                    arguments = new CmdArgument[]{new CmdArgument("client ip", true), new CmdArgumentTuple(true)};
                }
                @Override
                public void run(String base, ArrayList<String> args) {
                    String ipstring = args.get(0);
                    byte[] packet = new byte[args.size()-1];
                    for (int n = 1; n<args.size(); n++) {
                        packet[n-1] = Integer.valueOf(args.get(n)).byteValue();
                    }
                    
                    try {
                        InetAddress sendAddress = InetAddress.getByName(ipstring);
                        boolean found = false;
                        for (Socket client : clientList) {
                            if (client.getInetAddress().equals(sendAddress)) {
                                client.getOutputStream().write(packet);
                                found = true;
                                break;
                            }
                        }
                        if (found){
                            logEvent("Successfully sent bytes");
                        }else{
                            logEvent("Failed to send bytes");
                        }
                    } catch (Exception ex) {
                        logEvent("Failed to send bytes");
                        Logger.getLogger(MainInterface.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        );
        
        cm.addCommand("echobytes", 
            new CmdRunnable() {
                {
                    arguments = new CmdArgument[]{ new CmdArgumentTuple(true) };
                }
                @Override
                public void run(String baseString, ArrayList<String> args) {
                    byte[] bytesCont  = new byte[1024];
                    
                    int byteNum = 0;
                    for (int n = 0; n < args.size(); n++) {
                        String arg = args.get(n);
                        byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
                        
                        for (int n2 = 0; n2<bytes.length; n2++) {
                            bytesCont[byteNum] = bytes[n2];
                            byteNum++;
                        }
                        bytesCont[byteNum] = 32;
                        byteNum++;
                    }
                    String byteString = "";
                    for (int n = 0; n < byteNum-1; n++) {
                        byteString += Byte.toString(bytesCont[n]) + " ";
                    }
                    
                    logEvent(byteString);
                }
            }
        );
    }
    
    //<editor-fold defaultstate="collapsed" desc="Log Event Stuff">
    public void logEvent(String eventDescription) {
        LocalTime lt = LocalTime.now();
        eventDescription = "[" + lt.toString() + "] " + eventDescription;
        System.out.println(eventDescription);
        try {
            consoleLog += eventDescription + "\n";
        }catch(NullPointerException e) {
            Logger.getLogger(MainInterface.class.getName()).log(Level.SEVERE, null, e);
        }
        updateEventLog();
    }
    
    public void clearConsole(){
        consoleLog = "";
        updateEventLog();
    }
    
    public void updateEventLog() {
        consoleTextArea.setText(consoleLog);
    }
    
    public String getConsoleLog(){
        return consoleLog;
    }
    
    //</editor-fold>
    
    // update client list
    public void updateClientConnectionList() {
        dlm.clear();
        
        for (Socket client : clientList) {
            if (client.isConnected()) {
                InetAddress clientAddress = client.getInetAddress();
                String nickName = getNickname(clientAddress);
                dlm.addElement(nickName + " - " + clientAddress.toString());
            }else{
                clientList.remove(client);
            }
        }
        
        if (clientList.isEmpty()) {
            dlm.addElement("There are currently no connected clients.");
        }
        
        connectionsList.setModel(dlm);
    }
    
    public HashMap<InetAddress, ClientInfo> getClientInfo() {
        return cInfo;
    }
    
    
    // standard close connection with socket
    public void closeConnect(Socket clientSocket, boolean c) {
        try{
            if (c) {
                
                IOHandler.sendRequest(clientSocket, 126);
            }
            clientSocket.close();
            
            cInfo.remove(clientSocket.getInetAddress());
            clientList.remove(clientList.indexOf(clientSocket));
        }catch(IOException ex) {
            if(debug)
                Logger.getLogger(this.getName()).log(Level.SEVERE, null, ex);
        }
        updateClientConnectionList();
        updateClientInfo();
    }
    
    public void enterMessage() {
        byte[] address = {0,0,0,0};
        byte[] stringBytes = messageTextField.getText().getBytes(StandardCharsets.UTF_8);
        byte[] data = ByteMath.concatByteArray(address, stringBytes);
        for (Socket client : clientList) {
            try {
                IOHandler.sendPacket(client, data, 4);
            } catch (IOException ex) {
                Logger.getLogger(MainInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        msg.addMessage(new Message(messageTextField.getText()));
        
        updateConversationBox();
        messageTextField.setText("");
        messageTextField.requestFocus();
    }
    
    public void updateConversationBox() {
        String text = "";
        
        for (Message m : msg.getMessages()) {
            text += m.toString(this) + "\n";
        }
        
        conversationBox.setText(text);
    }
    
    // init connection with client, read input from server, respond accordingly 
    public void initializeConnectionWithClient(Socket clientSocket) throws IOException { 
        InetAddress clientAddress = clientSocket.getInetAddress();
        
        cInfo.put(clientAddress, new ClientInfo(clientSocket));
        
        updateClientConnectionList();
        
        logEvent("Reading from " + clientAddress + ":" + port);
        main: 
        while (true) {
            
            Packet packet = IOHandler.readPacket(clientSocket);
            byte[] data = packet.getData();
            
            switch (packet.getProtocol()) {
                case 1:
                    logEvent("Ping request recieved");
                    break;
                case 2:
                    String message = ByteMath.bytesToString(data);
                    
                    ArrayList<Byte> messageData = new ArrayList();
                    
                    if (!clientSocket.isConnected())
                        break main;
                    
                    for (byte b : clientAddress.getAddress()) {
                        messageData.add(b);
                    }
                    
                    for (byte b : data) {
                        messageData.add(b);
                    }
                    
                    for (Socket client : clientList) {
                        IOHandler.sendPacket(client, ByteMath.toArray(messageData), 2);
                    }
                    
                    msg.addMessage(new Message(message,clientAddress));
                    updateConversationBox();
                    break;
                case 3:
                    sendClientInfo(clientSocket);
                    break;
                case 4:
                    String nick = ByteMath.bytesToString(data);
                    logEvent("nickname change...");
                    for (ClientInfo c : cInfo.values()) {
                        if (c.nickname != null && c.nickname.equals(nick)) {
                            logEvent(clientAddress.toString() + " tried using nickname \"" + nick + "\" which was already taken.");
                            IOHandler.sendRequest(clientSocket, 125);
                            break main;
                        }
                    }
                    ClientInfo info = cInfo.get(clientAddress);
                    info.nickname = nick;
                    
                    logEvent(clientAddress.toString() + " - new nickname: " + ByteMath.bytesToString(data));
                    
                    ArrayList<Byte> updateInfoPacket = new ArrayList();
                    
                    for (byte b : clientAddress.getAddress()) {
                        updateInfoPacket.add(b);
                    }
                    byte[] joinTimeBytes = String.valueOf(info.joinTime).getBytes(StandardCharsets.UTF_8);
                    for (byte b : joinTimeBytes) {
                        updateInfoPacket.add(b);
                    }
                    updateInfoPacket.add((byte)-4);
                    byte[] nicknameBytes = nick.getBytes();
                    for (byte b : nicknameBytes) {
                        updateInfoPacket.add(b);
                    }
                    
                    for (Socket client : clientList) {
                        if (!client.equals(clientSocket)) {
                            IOHandler.sendPacket(client, ByteMath.toArray(updateInfoPacket), 5);
                            IOHandler.sendPacket(client, ByteMath.concatByteArray(new byte[]{0}, clientAddress.getAddress()), 6);
                        }
                    }
                    
                    break;
                case 126:
                    logEvent("Ending connection with " + clientAddress.toString() + " (" + cInfo.get(clientAddress).nickname + ")");
                    for (Socket client : clientList) {
                        if (!client.equals(clientSocket)) {
                            IOHandler.sendPacket(client, ByteMath.concatByteArray(new byte[]{1}, clientAddress.getAddress()), 6);
                        }
                    }
                    break main;
                case 127:
                    logEvent("Empty packet...");
                    break;
                default:
                    logEvent("Unknown protocol: " + packet.getProtocol());
                    break;
                    
            }
            
            sleep(200);
        }
        
        logEvent("Lost connection to " + clientAddress.toString());
        
        closeConnect(clientSocket, false);
    }
    
    public void sendClientInfo(Socket clientSocket) throws IOException {
        ArrayList<Byte> clientInfoData = new ArrayList();
                    
        for (Socket client : clientList) {
            InetAddress address = client.getInetAddress();
            ClientInfo info = cInfo.get(address);

            for (byte b : address.getAddress()) {
                clientInfoData.add(b);
            }

            byte[] joinTimeBytes = String.valueOf(info.joinTime).getBytes(StandardCharsets.UTF_8);
            for (byte b : joinTimeBytes) {
                clientInfoData.add(b);
            }
            clientInfoData.add((byte)-4);
            byte[] nicknameBytes = info.nickname.getBytes();
            for (byte b : nicknameBytes) {
                clientInfoData.add(b);
            }
            clientInfoData.add((byte)-3);
        }

        clientInfoData.add((byte)-2);

        IOHandler.sendPacket(clientSocket, ByteMath.toArray(clientInfoData), 3);
    }
    
    public void openConnection() {
        Thread connectionListenerThread = new Thread(
            () -> {
                try {
                    ServerSocket = new ServerSocket(port,maxClients);
                    
                    while (true) {
                        Socket clientSocket = ServerSocket.accept();
                        InetAddress clientAddress = clientSocket.getInetAddress();
                        logEvent("Accepted client " + clientAddress + ":" + port);
                        if (clientSocket.isConnected()) {
                            clientList.add(clientSocket);
                            logEvent("Connected to client " + clientAddress + ":" + port);
                            
                            Thread initClientConnection = new Thread(
                                () -> {
                                    try {
                                        initializeConnectionWithClient(clientSocket);
                                    } catch (Exception ex) {
                                        logEvent("Lost connection to client " + clientAddress + ":" + port + " | " + ex.getMessage());
                                        updateClientConnectionList();
                                        Logger.getLogger(MainInterface.class.getName()).log(Level.SEVERE, null, ex); 
                                    }
                                }
                            );
                            
                            initClientConnection.start();
                        }
                    }
                    
                } catch (IOException ex) {
                    logEvent("No longer accepting connections.");
                }
        });
        
        connectionListenerThread.start();
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        onlineButton = new javax.swing.JButton();
        connectionsTab = new javax.swing.JTabbedPane();
        consolePanel = new javax.swing.JPanel();
        consoleInputField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        consoleTextArea = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        connectionsPanel = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        connectionsList = new javax.swing.JList<>();
        clientPanel = new javax.swing.JPanel();
        jSplitPane3 = new javax.swing.JSplitPane();
        clientInfoPanel = new javax.swing.JPanel();
        disconnectClientButton = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        clientInfo = new javax.swing.JTextArea();
        sendSoundPanel = new javax.swing.JPanel();
        playButton = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        musicList = new javax.swing.JList<>();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        conversationBox = new javax.swing.JTextArea();
        messageTextField = new javax.swing.JTextField();
        sendMessage = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Server - Console/Connection Management");

        onlineButton.setText("Go Online");
        onlineButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                onlineButtonMouseClicked(evt);
            }
        });

        connectionsTab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                connectionsTabStateChanged(evt);
            }
        });

        consoleInputField.setText("sendbytes localhost 2 127 0 0 1 119 119 119 -2");
        consoleInputField.setToolTipText("Enter a console command here.");

        consoleTextArea.setEditable(false);
        consoleTextArea.setColumns(20);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setRows(5);
        consoleTextArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(consoleTextArea);

        jLabel2.setText(">");

        javax.swing.GroupLayout consolePanelLayout = new javax.swing.GroupLayout(consolePanel);
        consolePanel.setLayout(consolePanelLayout);
        consolePanelLayout.setHorizontalGroup(
            consolePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(consolePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(consoleInputField))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
        );
        consolePanelLayout.setVerticalGroup(
            consolePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(consolePanelLayout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addGroup(consolePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(consoleInputField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(8, 8, 8)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE))
        );

        connectionsTab.addTab("Console", consolePanel);

        jSplitPane2.setDividerLocation(150);

        connectionsList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                connectionsListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(connectionsList);

        jSplitPane2.setLeftComponent(jScrollPane2);

        clientPanel.setBackground(new java.awt.Color(204, 204, 204));

        jSplitPane3.setDividerLocation(250);

        disconnectClientButton.setText("Disconnect");
        disconnectClientButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                disconnectClientButtonMouseClicked(evt);
            }
        });

        clientInfo.setEditable(false);
        clientInfo.setColumns(20);
        clientInfo.setRows(5);
        jScrollPane3.setViewportView(clientInfo);

        javax.swing.GroupLayout clientInfoPanelLayout = new javax.swing.GroupLayout(clientInfoPanel);
        clientInfoPanel.setLayout(clientInfoPanelLayout);
        clientInfoPanelLayout.setHorizontalGroup(
            clientInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(disconnectClientButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE)
        );
        clientInfoPanelLayout.setVerticalGroup(
            clientInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(clientInfoPanelLayout.createSequentialGroup()
                .addComponent(disconnectClientButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE))
        );

        jSplitPane3.setLeftComponent(clientInfoPanel);

        playButton.setText("Play");
        playButton.setActionCommand("playButton");

        musicList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane5.setViewportView(musicList);

        javax.swing.GroupLayout sendSoundPanelLayout = new javax.swing.GroupLayout(sendSoundPanel);
        sendSoundPanel.setLayout(sendSoundPanelLayout);
        sendSoundPanelLayout.setHorizontalGroup(
            sendSoundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(playButton, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
            .addComponent(jScrollPane5)
        );
        sendSoundPanelLayout.setVerticalGroup(
            sendSoundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendSoundPanelLayout.createSequentialGroup()
                .addComponent(playButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE))
        );

        jSplitPane3.setRightComponent(sendSoundPanel);

        javax.swing.GroupLayout clientPanelLayout = new javax.swing.GroupLayout(clientPanel);
        clientPanel.setLayout(clientPanelLayout);
        clientPanelLayout.setHorizontalGroup(
            clientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane3)
        );
        clientPanelLayout.setVerticalGroup(
            clientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane3)
        );

        jSplitPane2.setRightComponent(clientPanel);

        javax.swing.GroupLayout connectionsPanelLayout = new javax.swing.GroupLayout(connectionsPanel);
        connectionsPanel.setLayout(connectionsPanelLayout);
        connectionsPanelLayout.setHorizontalGroup(
            connectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
        );
        connectionsPanelLayout.setVerticalGroup(
            connectionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2)
        );

        connectionsTab.addTab("Connections", connectionsPanel);

        conversationBox.setColumns(20);
        conversationBox.setRows(5);
        jScrollPane6.setViewportView(conversationBox);

        messageTextField.setToolTipText("Enter a message here");

        sendMessage.setText("Send");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(messageTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendMessage)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(messageTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendMessage))
                .addContainerGap())
        );

        connectionsTab.addTab("Messages", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(connectionsTab)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(onlineButton, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(onlineButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(connectionsTab)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onlineButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_onlineButtonMouseClicked
        Thread thread = new Thread(
            () -> {
                if (!online) {
                    openConnection();
                    online = true;
                    logEvent("Server online.");
                    onlineButton.setText("Go Offline");
                }else{
                    online = false;

                    try {
                        ServerSocket.close();
                    } catch (IOException ex) {
                        Logger.getLogger(MainInterface.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    for (Socket client : clientList) {
                        closeConnect(client, true);
                        logEvent("Disconnecting client " + client.getInetAddress() + ":" + port);
                    }
                    logEvent("Server offline.");
                    onlineButton.setText("Go Online");
                    updateClientConnectionList();
                }
            }
        );
        thread.start();
    }//GEN-LAST:event_onlineButtonMouseClicked

    public void updateClientInfo() {
        if (selectedClient != null) {
            InetAddress clientAddress = selectedClient.getInetAddress();
            String nickName = getNickname(clientAddress);
            
            int time = (Math.round(((int)System.currentTimeMillis()-getTimeJoined(clientAddress))/100)/10);
            
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
            String clientInfoText = "IP: " + clientAddress.toString().substring(1) + "\n"
                    + "Nickname: " + nickName + "\n"
                    + "Total Time Since Joined: " + timestring + "\n"
                    + "";

            clientInfo.setText(clientInfoText);
        }else if (!clientInfo.getText().equals("Select a client to see info about them.")){
            clientInfo.setText("Select a client to see info about them.");
        }
    }
    
    private void connectionsListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_connectionsListValueChanged
        if (evt.getValueIsAdjusting() == false) {
            String listValue = connectionsList.getSelectedValue();
            if (listValue == null || listValue.equals("There are currently no connected clients.")){
                return;
            }
            Socket clientSocket = null;
            for (Socket cs : clientList) {
                if (listValue.equals(getNickname(cs.getInetAddress()) + " - " + cs.getInetAddress())) {
                    clientSocket = cs;
                    break;
                }
            }
            if (clientSocket == null) {
                selectedClient = null;
                logEvent("Something unexpected happened!");
                return;
            }
            selectedClient = clientSocket;
            updateClientInfo();
        }
    }//GEN-LAST:event_connectionsListValueChanged

    private void disconnectClientButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_disconnectClientButtonMouseClicked
        if (selectedClient != null) {
            closeConnect(selectedClient, true);
        }
    }//GEN-LAST:event_disconnectClientButtonMouseClicked

    private void connectionsTabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_connectionsTabStateChanged
        updateClientInfo();
        updateClientConnectionList();
    }//GEN-LAST:event_connectionsTabStateChanged
    
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
                System.out.println(info.getName());
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainInterface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new MainInterface().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea clientInfo;
    private javax.swing.JPanel clientInfoPanel;
    private javax.swing.JPanel clientPanel;
    private javax.swing.JList<String> connectionsList;
    private javax.swing.JPanel connectionsPanel;
    private javax.swing.JTabbedPane connectionsTab;
    private javax.swing.JTextField consoleInputField;
    private javax.swing.JPanel consolePanel;
    private javax.swing.JTextArea consoleTextArea;
    private javax.swing.JTextArea conversationBox;
    private javax.swing.JButton disconnectClientButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JTextField messageTextField;
    private javax.swing.JList<String> musicList;
    private javax.swing.JButton onlineButton;
    private javax.swing.JButton playButton;
    private javax.swing.JButton sendMessage;
    private javax.swing.JPanel sendSoundPanel;
    // End of variables declaration//GEN-END:variables
    public JTextField cif;
    public JTextArea  cta;
}
