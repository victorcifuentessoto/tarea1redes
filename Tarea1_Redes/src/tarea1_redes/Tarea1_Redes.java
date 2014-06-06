package tarea1_redes;

import java.io.*;
import java.net.*;
import java.util.Random;


public final class Tarea1_Redes {
    
    private static int port = 8091;
    private static String host = "localhost";
    
    //Esta función retorna el server socket http con su respectivo puerto aleatorio
    public static ServerSocket httpServer(){
        Random rand = new Random();
        int HTTPport = rand.nextInt(42001) + 8000;
        while(true){
            try {
                return new ServerSocket(HTTPport);
            } catch (IOException ex) {
                HTTPport = rand.nextInt(42001) + 8000; /* Se escoje otro puerto en caso de 
                                                        que el puerto anterior no esté disponible*/
                
            }
        }
    }
    
    private static String getNick(BufferedReader consola, BufferedReader reader, PrintWriter writer, ServerSocket httpserver) throws IOException{
        System.out.println("Ingrese su nick: ");
        String nick = consola.readLine();
        writer.println("NICK " + nick + " " + httpserver.getLocalPort());
        String respuestaServidor = reader.readLine();
        if ("SERVIDOR: MENSAJE ENVIADO".equals(respuestaServidor))
            return nick;
        System.out.println(respuestaServidor);
        return getNick(consola, reader, writer, httpserver);
    }
       
    public static void main(String[] args) throws Exception {
        //Se crea el servidor TCP y el servidor http.
        BufferedReader consola = new BufferedReader(new InputStreamReader(System.in));
        Socket servidor = new Socket(host, port);
        PrintWriter writer = new PrintWriter(servidor.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(servidor.getInputStream()));
        //httpServer retornara el servidor Socket con un puerto que no se encuentre ocupado.
        ServerSocket listener = httpServer();
        //Ingreso del nick del cliente.
        getNick(consola, in, writer, listener);
        
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
