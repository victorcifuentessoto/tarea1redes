package tarea1_redes;

import java.net.*;


public final class Tarea1_Redes {
    public static void main(String[] args) throws Exception {
        int port = 8090;
        //Se crea el servidor socket.
        ServerSocket listener = new ServerSocket(port);
        //Espera respuesta del cliente.
        while(true){
            Socket connectionSocket = listener.accept();
            HttpRequest request = new HttpRequest(connectionSocket);
            //Uso de multi hebras para el uso del servidor.
            Thread thread = new Thread(request);
            thread.start();
        }
    }
    
}
