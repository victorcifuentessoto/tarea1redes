package tarea1_redes;

import java.io.*;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.jespxml.JespXML;
import org.jespxml.excepciones.AtributoNotFoundException;
import org.jespxml.excepciones.TagHijoNotFoundException;
import org.jespxml.modelo.Atributo;
import org.jespxml.modelo.Tag;
import org.xml.sax.SAXException;
/*
//<librerias para el XML
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
//librerias para el XML>
*/
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int ch;

        //Escritura del request message al stream del socket
        while((ch = is.read()) != -1) {
            outputStream.write(ch);
            if (ch == '\n') {
                ch = is.read();
                if (ch == '\n' || ch == '\r')
                    break;
                outputStream.write(ch);
            }
        }
        
        //Guarda el request message.
        String headerRequest = new String(outputStream.toByteArray(), "UTF-8");

        // Seteando inputs.
  
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        //String tokens para extraer el nombre del archivo y el metodo (GET o POST)
        StringTokenizer tokens = new StringTokenizer(headerRequest);
        String method = tokens.nextToken();  //Se obtiene el metodo
        String fileName = tokens.nextToken();  //Se obtiene el nombre del archivo
        //Se antepone un punto para que el archivo lo encuentre en el directorio actual.
        fileName = "." + fileName;
        
        if(method.equals("POST")){
            outputStream = new ByteArrayOutputStream();
            ch = is.read();
            while((ch = is.read()) != -1) {
                outputStream.write(ch);
                if (ch == '\n' || ch == '\r' || ch == '4'){
                    break;
                }
            }
            String payload = new String(outputStream.toByteArray(), "UTF-8");
            //puedes imprimirla si quieres:
            System.out.println(payload);
            //extraccion de data de payload

             //YO CAMBIANDO COSAS
           //se separaron las variables
            String[] Personas = payload.split("&");
            
            for (int i = 0; i < Personas.length; i++) {
                System.out.println(Personas[i]);
            }
            //Holi aca va lo del XML
            
            try {
             //creo el objeto JespXML con el archivo que quiero crear
             JespXML archivo = new JespXML("contactos.xml");
             
             //declaro el Tag raiz, que en esta caso se llama contactos
             Tag contactos = new Tag("contactos");
             
             //creo el Tag contacto, que va a tener un nombre, ip y puerto
             Tag contacto = new Tag("contacto");
             Tag nombre, IP, puerto;
             
             //construyo los Tags nombre , IP y puerto y le agrego contenido
             nombre = new Tag("nombre");
             IP = new Tag("IP");
             puerto = new Tag("puerto");
             nombre.addContenido(Personas[0]);
             IP.addContenido(Personas[1]);
             puerto.addContenido(Personas[2]);
             
             //agrego el Tag nombre, ip  puerto al Tag contacto
             contacto.addTagHijo(nombre);
             contacto.addTagHijo(IP);
             contacto.addTagHijo(puerto);
             
             //finalmente agrego al Tag contactos, el tag contacto
             contactos.addTagHijo(contacto);
             //y escribo el archivo XML
             archivo.escribirXML(contactos);
         } catch (ParserConfigurationException | FileNotFoundException ex) {
             Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
         } catch (TransformerConfigurationException ex) {
             Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
         } catch (TransformerException ex) {
             Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
         }
            
            
   
        //Leer el XML########################################################
            //listacontactos = new ArrayList<>();
            try {
             //Cargo el archivo
             JespXML archivo = new JespXML("contactos.xml");
             //leo el archivo y me retorna el tag raiz, que en este caso
             // es contactos
             Tag contactos = archivo.leerXML();
             
             
             //Obtengo los tags que necesito, por el nombre
                Tag contacto = contactos.getTagHijoByName("contacto");
                Tag nombre = contacto.getTagHijoByName("nombre");
                String nombrecontacto;
                nombrecontacto = nombre.getContenido();
                System.out.println("nombre: "+nombrecontacto);
                     
             //imprimo la información requerida
             
         } catch (ParserConfigurationException | IOException | SAXException ex) {
             //exception lanzada cuando no se encuentra el atributo
             Logger.getLogger(HttpRequest.class.getName()).log(Level.SEVERE, null, ex);
         }
        //########################################################Leer el XML
            
            
            //HASTA ACA
            
            
        }
        
        

        // Se abre el archivo.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }   

        //Construcción del mensaje de respuesta http.
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
            entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>PAGE NOT FOUND</BODY></HTML>";
        }

        // Envío del statusLine.
        os.writeBytes(statusLine);

        // Envío del tipo de contenido.
        os.writeBytes(contentTypeLine);

        // Envío de una línea en blanco para indicar el final de header line.
        os.writeBytes(CRLF);

        // Envío del cuerpo de la página.
        if (fileExists) {
            sendBytes(fis, os); //Si la página existe se envian las líneas del archivo
                                //encontrado.
            fis.close();
        }else{
            os.writeBytes(entityBody);  //Sino se escribe las líneas del archivo no encontrad (Not found).
        }

        os.close(); //Cierre de streams y socket.
        br.close();
        socket.close();

    }

    //Need this one for sendBytes function called in processRequest
    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        // Construcción de 1 KB de buffer para el socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copia el requested file aloutput del socket.
        while((bytes = fis.read(buffer)) != -1 ) {
           os.write(buffer, 0, bytes);
        }
    }
    
    //Esta función obtiene el tipo de archivo pedido.
    private static String contentType(String fileName)
    {
        if(fileName.endsWith(".htm") || fileName.endsWith(".html"))
            return "text/html";
        return "application/octet-stream";
    }
}