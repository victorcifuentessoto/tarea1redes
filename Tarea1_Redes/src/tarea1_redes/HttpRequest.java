package tarea1_redes;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import tarea1_redes.Contacto.ForSaveMultiple;

public final class HttpRequest implements Runnable {
 
    final static String CRLF = "\r\n";
    String[] list_contactos;
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
        //OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), "8859_1");
        int bytes;

        //Escritura del request message al stream del socket
        while((bytes = is.read()) != -1) {
            outputStream.write(bytes);
            if (bytes == '\n') {
                bytes = is.read();
                if (bytes == '\n' || bytes == '\r')
                    break;
                outputStream.write(bytes);
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
            while(true){
                String context_length = tokens.nextToken();
                if(context_length.equals("Content-Length:"))
                    break;
            }

            int contador_form_data = Integer.parseInt(tokens.nextToken());
            
            outputStream = new ByteArrayOutputStream();
            bytes = is.read();
            for(int i = 0; i < contador_form_data; i++) {
                bytes = is.read();
                outputStream.write(bytes);
            }
            //extraccion de data de payload
            String payload = new String(outputStream.toByteArray(), "UTF-8");
            

            //se separaron las variables
            String[] Personas = payload.split("&");
            
            //Se crea la clase para archivos xml
            ForSaveMultiple forSaveMultiple = new ForSaveMultiple();
            
            //Trabajando con archivos xml para guardar/mostrar contactos.
            
            try {
                //Se abre el archivo xml de los contactos registrados.
                File fXmlFile = new File("contactos.xml");
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                //Conversión del archivo. En caso de error se crea un nuevo contactos.xml
                Document doc = dBuilder.parse(fXmlFile);
                
                //Se obtiene la lista de contactos por cada nodo <contacto> en el archivo.
                NodeList lista_contactos = doc.getElementsByTagName("contacto");
                
                //Normaliza el archivo.
                doc.getDocumentElement().normalize();
                
                for (int i = 0; i < lista_contactos.getLength(); i++) {
                    Node nodo = lista_contactos.item(i);
                    
                    if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                        Element elemento = (Element) nodo;
                        
                        //Guardamos los datos del contacto del nodo actual en la clase contacto.
                        Contacto contacto = new Contacto();
                        
                        contacto.setNombre(elemento.getElementsByTagName("nombre").item(0).getTextContent());
                        contacto.setDireccion_ip(elemento.getElementsByTagName("direccion_ip").item(0).getTextContent()); 
                        contacto.setPuerto(elemento.getElementsByTagName("puerto").item(0).getTextContent());
                        forSaveMultiple.getList().add(contacto);
                    }
                }
                
                //Crea un contacto con sus datos
                Contacto contacto_nuevo = new Contacto();
                contacto_nuevo.setNombre(Personas[0].substring(7));
                contacto_nuevo.setDireccion_ip(Personas[1].substring(13));
                contacto_nuevo.setPuerto(Personas[2].substring(7));
                forSaveMultiple.getList().add(contacto_nuevo);
                
                JAXBContext jaxb = JAXBContext.newInstance( ForSaveMultiple.class );

                Marshaller marshaller = jaxb.createMarshaller();

                marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
                
                File file = new File( "contactos.xml" );
                marshaller.marshal( forSaveMultiple, file );
                
        //Caso de no encontrar el archivo.     
            } catch (ParserConfigurationException | FileNotFoundException ex) {
                Contacto contacto_nuevo = new Contacto();
                contacto_nuevo.setNombre(Personas[0].substring(7));
                contacto_nuevo.setDireccion_ip(Personas[1].substring(13));
                contacto_nuevo.setPuerto(Personas[2].substring(7));
                forSaveMultiple.getList().add(contacto_nuevo);
                
                JAXBContext jaxb = JAXBContext.newInstance( ForSaveMultiple.class );

                Marshaller marshaller = jaxb.createMarshaller();

                marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
                
                File file = new File( "contactos.xml" );
                marshaller.marshal( forSaveMultiple, file );
            }
        }
        else if(method.equals("GET") && fileName.equals("./vercontacto.html")){
            //Se abre el archivo xml de los contactos registrados.
            File fXmlFile = new File("contactos.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            //Conversión del archivo. En caso de error se crea un nuevo contactos.xml
            Document doc = dBuilder.parse(fXmlFile);

            //Se obtiene la lista de contactos por cada nodo <contacto> en el archivo.
            NodeList lista_contactos = doc.getElementsByTagName("contacto");

            //Normaliza el archivo.
            doc.getDocumentElement().normalize();

            list_contactos = new String[lista_contactos.getLength()];
            
            for (int i = 0; i < lista_contactos.getLength(); i++) {
                Node nodo = lista_contactos.item(i);

                if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                    Element elemento = (Element) nodo;

                    Contacto contacto = new Contacto();

                    contacto.setNombre(elemento.getElementsByTagName("nombre").item(0).getTextContent());
                    contacto.setDireccion_ip(elemento.getElementsByTagName("direccion_ip").item(0).getTextContent()); 
                    contacto.setPuerto(elemento.getElementsByTagName("puerto").item(0).getTextContent());
                    
                    list_contactos[i] = contacto.getNombre();
                }
            }
            /*writer.write("<div class=\"form-group\">\n" +
                        "<label class=\"sr-only\" for=\"exampleInputconectados\"></label>\n" +
                        "<select multiple class=\"form02\">");
            for(int i = 0; i < list_contactos.length; i++){
                writer.write("<option>"+list_contactos[i]+"</option>");
            }
            writer.write("</select>");*/
        }

        // Se abre el archivo html.
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
            os.writeBytes(entityBody);  //Sino se escribe las líneas del archivo no encontrado (Not found).
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

        // Copia el requested file al output del socket.
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