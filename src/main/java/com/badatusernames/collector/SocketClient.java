package com.badatusernames.collector;

import java.io.*;
import java.net.*;

public class SocketClient {
    private Socket socket;
    private DataOutputStream out;
    private BufferedReader in;
    private final String serverIP;
    private final int serverPort;

    public SocketClient(String ip, int port) {
        this.serverIP = ip;
        this.serverPort = port;
    }

    public void startConnection() throws IOException {
        socket = new Socket(serverIP, serverPort);
        out = new DataOutputStream(socket.getOutputStream());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Message requires reading response from server
    public void sendMessage(String msg) throws IOException {
        msg += "\n"; // Append newline character to each message
        out.writeBytes(msg); // Send the message
        System.out.println("Server says: " + in.readLine()); // Print server response
    }

    // Command does not require reading response from server
    public void sendCommand(String command) throws IOException {
        command += "\n";
        out.writeBytes(command);
    }

    public void stopConnection() throws IOException {
        sendCommand("!DISCONNECT");
        in.close();
        out.close();
        socket.close();
    }

}
