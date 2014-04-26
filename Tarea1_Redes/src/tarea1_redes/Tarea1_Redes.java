package tarea1_redes;

import java.util.*;
import java.io.*;
import java.net.*;


public final class Tarea1_Redes {
    public static void main(String[] args) throws Exception {
        int port = 8090;
        ServerSocket listener = new ServerSocket(port);
        while(true){
            Socket connectionSocket = listener.accept();
            HttpRequest request = new HttpRequest(connectionSocket);
            Thread thread = new Thread(request);
            thread.start();
        }
    }
    
}
