/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tarea1_redes;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author Victor
 */
public final class Tarea1_Redes {

    /**
     * @param args the command line arguments
     */
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
