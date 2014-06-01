package tarea1_redes;

import java.net.*;


public final class Tarea1_Redes {
    
    private static int port = 8091;
    private static String host = "localhost";
    
    public static void main(String[] args) throws Exception {
        //Se crea el servidor TCP y el servidor http.
        Socket servidor = new Socket(host, port);
        ServerSocket listener = new ServerSocket(8090);
        //Espera respuesta del cliente.
        while(true){
            Socket connectionSocket = listener.accept();
            HttpRequest request = new HttpRequest(connectionSocket, servidor);
            //Uso de multi hebras para el uso del servidor.
            Thread thread = new Thread(request);
            thread.start();
        }
    }
    
}
