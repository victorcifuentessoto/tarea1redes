package tarea1_redes;

import java.io.*;
import java.net.*;
import static java.net.URLDecoder.decode;
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
    Socket servidor;
    String respuestaServidor;
    String mensaje_mejorado;
    String historial = ""; //Muestra en el textarea el historial cliente-servidor.

    // Constructor
    public HttpRequest(Socket httpServer, Socket servidorTCP) throws Exception
    {
       this.socket = httpServer;
       this.servidor = servidorTCP;
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
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(servidor.getInputStream()));
        DataOutputStream outToServer = new DataOutputStream(servidor.getOutputStream());
        InputStream is = socket.getInputStream();
        InputStream inputFile = servidor.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
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
            if(payload.contains("mensaje")){
                
                String[] substringsMensaje = payload.split("=");
                String mensaje = substringsMensaje[1];
                mensaje_mejorado = decode(mensaje, "UTF-8");;
                
                //Escribe el mensaje al servidor
                outToServer.writeBytes(mensaje_mejorado + "\n");
                //Recibe el mensaje del servidor.
                if(mensaje_mejorado.startsWith("MENSAJE")){
                    respuestaServidor = inFromServer.readLine();
                }
                else if (mensaje_mejorado.startsWith("ARCHIVO")) {  
                    byte [] mybytearray  = new byte [6022386];
                    int bytesRead;
                    int current = 0;
                    FileOutputStream fos = null;
                    BufferedOutputStream bos = null;
                    fos = new FileOutputStream("");
                    bos = new BufferedOutputStream(fos);
                    bytesRead = inputFile.read(mybytearray,0,mybytearray.length);
                    current = bytesRead;
                    do {
                        bytesRead = is.read(mybytearray, current, (mybytearray.length-current));
                        if(bytesRead >= 0) 
                            current += bytesRead;
                    } while(bytesRead > -1);

                    bos.write(mybytearray, 0 , current);
                    bos.flush();
                }
                
                //Creación del archivo que guarda historial de mensajes (textarea)
                File archivoHistorial = new File("Historial_" + socket.getLocalPort() + ".txt");
                if(archivoHistorial.exists() && !archivoHistorial.isDirectory()){
                    PrintWriter writer = new PrintWriter("temp.txt", "UTF-8");
                    BufferedReader reader = new BufferedReader(new FileReader("Historial_" + socket.getLocalPort() + ".txt"));
                    String line;
                    while((line = reader.readLine()) != null){
                        writer.println(line);
                    }
                    writer.println(respuestaServidor);
                    reader.close();
                    writer.close();
                    File tempFile = new File("temp.txt");
                    PrintWriter newWriterFile = new PrintWriter("Historial_" + socket.getLocalPort() + ".txt", "UTF-8");
                    BufferedReader readerTmp = new BufferedReader(new FileReader("temp.txt"));
                    while((line = readerTmp.readLine()) != null){
                        newWriterFile.println(line);
                        historial = historial + line + "\n";
                    }
                    newWriterFile.close();
                    readerTmp.close();
                    tempFile.delete();
                }
                else{
                    PrintWriter writer = new PrintWriter("Historial_" + socket.getLocalPort() + ".txt", "UTF-8");
                    writer.println(respuestaServidor);
                    writer.close();
                    historial = historial + respuestaServidor + "\n";
                }
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
            }
            else{
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

                    list_contactos = new String[lista_contactos.getLength() + 1];

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

                            list_contactos[i] = contacto.getNombre();
                        }
                    }

                    //Crea un contacto con sus datos
                    Contacto contacto_nuevo = new Contacto();
                    contacto_nuevo.setNombre(Personas[0].substring(7));
                    contacto_nuevo.setDireccion_ip(Personas[1].substring(13));
                    contacto_nuevo.setPuerto(Personas[2].substring(7));
                    forSaveMultiple.getList().add(contacto_nuevo);

                    list_contactos[lista_contactos.getLength()] = contacto_nuevo.getNombre();

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
        }
        else if(method.equals("GET") && fileName.equals("./vercontacto.html")){
            //Revisa si hay mensajes nuevos.
            String mensajes_nuevos = "";
            File archivoHistorial = new File("Historial_" + socket.getLocalPort() + ".txt");
            outToServer.writeBytes("UPDATE\n");
            if(archivoHistorial.exists() && !archivoHistorial.isDirectory()){
                BufferedReader reader = new BufferedReader(new FileReader("Historial_" + socket.getLocalPort() + ".txt"));
                PrintWriter writerTemp = new PrintWriter("temp.txt", "UTF-8");
                String line;
                while((line = reader.readLine()) != null){
                    historial = historial + line + "\n";
                    writerTemp.println(line);
                }
                reader.close();
                writerTemp.close();
                String line_mensajes_nuevos;
                File tempFile = new File("temp.txt");
                PrintWriter writer = new PrintWriter("Historial_" + socket.getLocalPort() + ".txt", "UTF-8");
                BufferedReader readerTmp = new BufferedReader(new FileReader("temp.txt"));
                while((line = readerTmp.readLine()) != null){
                    writer.println(line);
                }
                while(!"".equals(line_mensajes_nuevos = inFromServer.readLine())){
                    mensajes_nuevos = mensajes_nuevos + line_mensajes_nuevos + "\n";
                    writer.println(line_mensajes_nuevos);
                }
                readerTmp.close();
                writer.close();
                tempFile.delete();
            }
            else{
                String line_mensajes_nuevos;
                PrintWriter writer = new PrintWriter("Historial_" + socket.getLocalPort() + ".txt", "UTF-8");
                while(!"".equals(line_mensajes_nuevos = inFromServer.readLine())){
                    mensajes_nuevos = mensajes_nuevos + line_mensajes_nuevos + "\n";
                    writer.println(line_mensajes_nuevos);
                }
                writer.close();
            }
            historial = historial + mensajes_nuevos + "\n";
            
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
            if(fileName.equals("./vercontacto.html")){
                os.writeBytes("<!DOCTYPE html>\n" +
                    "<html lang=\"es\">\n" +
                    "<meta charset=\"utf-8\"> \n" +
                    "<head>\n" +
                    "\n" +
                    "<title>Avioncito de Papel</title>\n" +
                    "<link rel=\"stylesheet\" type=\"text/css\" href=\"css/bootstrap.css\">\n" +
                    "</head>\n" +
                    "<body background=\"http://www.stylishlife.co.uk/media/catalog/product/cache/1/image/9df78eab33525d08d6e5fb8d27136e95/v/i/vintage-contemporary-old-brick-white-wallpaper2_1.jpg\">\n" +
                    "\n" +
                    "<!-- BARRA DE NAVEGACIÓN-->\n" +
                    "<div class=\"container-fluid\">\n" +
                    "    <nav class=\"navbar navbar-default\" role=\"navigation\">\n" +
                    "        <!-- Brand and toggle get grouped for better mobile display -->\n" +
                    "        <div class=\"navbar-header\">\n" +
                    "          <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\"#bs-example-navbar-collapse-1\">\n" +
                    "            <span class=\"sr-only\">barra de navegación</span>\n" +
                    "            <span class=\"icon-bar\"></span>\n" +
                    "            <span class=\"icon-bar\"></span>\n" +
                    "            <span class=\"icon-bar\"></span>\n" +
                    "          </button>\n" +
                    "          <a class=\"navbar-brand\" href=\"#\">Redes</a>\n" +
                    "        </div>\n" +
                    "\n" +
                    "        <!-- Collect the nav links, forms, and other content for toggling -->\n" +
                    "        <div class=\"collapse navbar-collapse\" id=\"bs-example-navbar-collapse-1\">\n" +
                    "\n" +
                    "          <ul class=\"nav navbar-nav\">\n" +
                    "            <li><a href=\"index.html\">Ingresar Contacto</a></li>\n" +
                    "            <li class=\"active\"><a href=\"vercontacto.html\">Ver Contactos</a></li>\n" +
                    "          </ul>\n" +
                    "\n" +
                    "          <form class=\"navbar-form navbar-left\" role=\"search\">\n" +
                    "            <div class=\"form-group\">\n" +
                    "              <input type=\"text\" class=\"form-control\" placeholder=\"Buscar\">\n" +
                    "            </div>\n" +
                    "            <button type=\"submit\" class=\"btn btn-default\">Ir</button>\n" +
                    "          </form>\n" +
                    "        </div><!-- /.navbar-collapse -->\n" +
                    "    </nav>\n" +
                    "</div>\n" +
                    "\n" +
                    "<div class=\"col-md-11 col-md-offset-1\">\n" +
                    "<form class=\"form-inline\" role=\"form\" method=\"POST\">\n" +
                    "  <h2> Contactos</h2> \n" +
                    "  <div class=\"form-group\">\n" +
                    "    <label class=\"sr-only\" for=\"exampleInputconectados\"></label>\n" +
                    "        <select multiple class=\"form02\" style=\"margin: 0px -0.5px 0px 0px; width: 90px; height: 130px;\">\n");
                if(list_contactos != null){
                    for (String list_contacto : list_contactos) {
                        os.writeBytes("<option>" + list_contacto + "</option>\n");
                    }
                }

                os.writeBytes("</select>\n" +
                    "    </div>\n" +
                    "\n" +
                    "<div class=\"form-group\">\n" +
                "    <div class=\"row\">\n" +
                "      <div class=\"col-md-9\">\n" +
                "        <label class=\"sr-only\" for=\"exampleInputPassword2\"></label>\n" +
                "        <textarea readonly class=\"form-control\" rows=\"5\" style=\"margin: 0px -0.5px 0px 0px; width: 816px; height: 100px;\">" + historial + "</textarea>\n" +
                "        <div class=\"row\">\n" +
                "          <div class=\"col-md-6 form-group has-success\">\n" +
                "            <label class=\"control-label\" for=\"inputSuccess1\">escribe acá</label>\n" +
                "            <input type=\"text\" name=\"mensaje\" class=\"form-control\" id=\"inputSuccess1\">\n" +
                "          </div>\n" +
                "          <div class=\"col-md-0\">\n" +
                "            <button type=\"submit\" class=\"btn btn-success\">Enviar</button>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</form>\n" +
                "</div>\n" +
                "\n" +
                "<form action=\"index.html\">\n" +
                "<div class=\"col-md-10 col-md-offset-1\">\n" +
                "    <br>\n" +
                "    <button type=\"submit\" class=\"btn btn-default\"><a href=\"index.html\">Agregar Contactos</a></button>\n" +
                "    <br><br><br><br><br>\n" +
                "    <p>Redes de Computadores, Primer Semestre 2014</p>\n" +
                "</div>\n" +
                "</form>\n" +
                "<script>\n" +
                "    setInterval(function(){\n" +
                "		window.location = window.location.href;\n" +
                "		},20000);\n" +
                "</script></body>\n" +
                "</html>");
            }
            else{
                sendBytes(fis, os); //Si la página existe se envian las líneas del archivo
                                //encontrado.
            }
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