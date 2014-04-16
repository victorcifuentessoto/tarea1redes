package tarea1_redes;

import java.io.*;
import java.net.*;
import java.util.*;

public final class HttpRequest implements Runnable {
 
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor
    public HttpRequest(Socket socket) throws Exception
    {
       this.socket = socket;
    }

    public void run()
    {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
       }
    }

    private void processRequest() throws Exception
    {
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());  	

        // Set up input stream filters.
  
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String requestLine = br.readLine(); //Se obtiene la primera línea del request.
        System.out.println();  //imprime el request message
        System.out.println(requestLine);

        //String tokens para extraer el nombre del archivo y el metodo (GET o POST)
        StringTokenizer tokens = new StringTokenizer(requestLine);
        String method = tokens.nextToken();  //Se obtiene el metodo
        String fileName = tokens.nextToken();  //Se obtiene el nombre del archivo
        //Se antepone un punto para que el archivo lo encuentre en el directorio actual.
        fileName = "." + fileName;

        // Se abre el archivo.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }   

        //Construct the response message
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        if (fileExists) {
            statusLine = "HTTP/1.1 200 OK: ";
            contentTypeLine = "Content-Type: " +
            contentType(fileName) + CRLF;
        }else{
            statusLine = "HTTP/1.1 404 Not Found: ";
            contentTypeLine = "Content-Type: text/html" + CRLF;
            entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>Not Found</BODY></HTML>";
        }
        //End of response message construction

        // Send the status line.
        os.writeBytes(statusLine);

        // Send the content type line.
        os.writeBytes(contentTypeLine);

        // Send a blank line to indicate the end of the header lines.
        os.writeBytes(CRLF);

        // Send the entity body.
        if (fileExists) {
            sendBytes(fis, os);
            fis.close();
        }else{
            os.writeBytes(entityBody);
        }

        os.close(); //Close streams and socket.
        br.close();
        socket.close();

    }

    //Need this one for sendBytes function called in processRequest
    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copy requested file into the socket�s output stream.
        while((bytes = fis.read(buffer)) != -1 ) {
           os.write(buffer, 0, bytes);
        }
     }
    private static String contentType(String fileName)
    {
        if(fileName.endsWith(".htm") || fileName.endsWith(".html"))
            return "text/html";
        if(fileName.endsWith(".jpg"))
             return "text/jpg";
        if(fileName.endsWith(".gif"))
             return "text/gif";
        return "application/octet-stream";
    }
}