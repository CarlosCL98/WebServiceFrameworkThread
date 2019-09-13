package apps;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

/**
 * Service class allows to read every class and method
 * that a developer creates and given a request be able
 * to response correctly.
 */
public class Service {

    private static Map<String, Handler> urlHandler = new HashMap();
    private static int port = getPort();
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static BufferedOutputStream dataOut;

    /**
     * When listen is execute, all the connections between server and client start
     * and stay ready to receive requests.
     */
    public static void listen() {
        while (true) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Listening for connections on port --> " + port);
            } catch (IOException ex) {
                System.out.println("Could not listen on port: " + port + ". IOException: " + ex);
            }
            // Keep connection until client disconnect to the server.
            try {
                clientSocket = serverSocket.accept();
                System.out.println("Connection accepted.");
            } catch (IOException e) {
                System.out.println("Could not accept the connection to client.");
            }
            // Prepare to receive and send requests and responses.
            // For the header.
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // For the binary data requested.
                dataOut = new BufferedOutputStream(clientSocket.getOutputStream());
                // Header from client.
                String inputLine = in.readLine();
                String[] header = new String[]{"GET", "/", "HTTP/1.1"};
                // Read HTTP request from the client socket.
                int cantRead = 0;
                while (inputLine != null) {
                    if (cantRead < 1) {
                        header = inputLine.split(" ");
                    }
                    System.out.println("Received: " + inputLine);
                    if (!in.ready()) {
                        break;
                    }
                    inputLine = in.readLine();
                    cantRead++;
                }
                String[] request = header[1].split("/");
                if (request.length > 1) {
                    if ("apps".equals(request[1])) {
                        String req = "";
                        String res = "200 OK";
                        for (String r : request) {
                            if (!r.isEmpty()) {
                                req += r + "/";
                            }
                        }
                        // Look for param query
                        String methodArgs = "";
                        if (req.contains("?")) {
                            String[] params = req.substring(req.indexOf("?") + 1, req.length() - 1).split("&");
                            methodArgs = params[0].substring(params[0].indexOf("=") + 1, params[0].length());
                    /*methodArgs = new String[params.length];
                    for (int i = 0; i < params.length; i++) {
                        methodArgs[i] = params[i].substring(params[i].indexOf("=")+1, params[i].length());
                    }*/
                            req = req.substring(0, req.indexOf("?"));
                        } else {
                            req = req.substring(0, req.length() - 1);
                        }
                        if (urlHandler.containsKey(req)) {
                            // Header
                            HttpServer.headerResponse(out, null, "text/html", res);
                            // Content
                            String content = null;
                            if (methodArgs.equals("")) {
                                content = (urlHandler.get(req)).process();
                            } else {
                                content = (urlHandler.get(req)).process(methodArgs);
                            }
                            if (content == null) {
                                String[] newHeader = new String[]{"GET", "/notFound.html", "HTTP/1.1"};
                                HttpServer.httpHandler(newHeader, out, dataOut);
                            } else {
                                out.write(content + "\r\n");
                                out.flush();
                            }
                        } else {
                            String[] newHeader = new String[]{"GET", "/notFound.html", "HTTP/1.1"};
                            HttpServer.httpHandler(newHeader, out, dataOut);
                        }
                    } else {
                        HttpServer.httpHandler(header, out, dataOut);
                    }
                } else {
                    HttpServer.httpHandler(header, out, dataOut);
                }
                out.close();
                in.close();
                dataOut.close();
                clientSocket.close();
                serverSocket.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    /**
     * Add web methods add to a hash map all methods of
     * all classes with the @web annotation.
     *
     * @param className is a String with the name of a class.
     */
    public static void addWebMethods(String className) {
        try {
            Class<?> c = Class.forName(className);
            Method[] methods = c.getMethods();
            for (Method m : methods) {
                if (m.isAnnotationPresent(Web.class)) {
                    urlHandler.put("apps/" + m.getAnnotation(Web.class).value(), new StaticMethodHandler(m));
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Init fill the hash map with the handlers to
     * all methods of each class in the classpath.
     */
    public static void init() {
        String path = "apps";
        ArrayList<File> directories = new ArrayList<File>();
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            // Ask for all resources for the path
            Enumeration<URL> resources = cld.getResources(path);
            while (resources.hasMoreElements()) {
                directories.add(new File(URLDecoder.decode(resources.nextElement().getPath(), "UTF-8")));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (File directory : directories) {
            if (directory.exists()) {
                for (String file : directory.list()) {
                    if (file.endsWith(".class")) {
                        String fileWithoutClass = file.substring(0, file.indexOf("."));
                        addWebMethods(path + "." + fileWithoutClass);
                    }
                }
            }
        }
    }

    /**
     * This method reads the default port as specified by the PORT variable in
     * the environment.
     *
     * Heroku provides the port automatically so you need this to run the
     * project on Heroku.
     */
    private static int getPort() {
        port = 40000; //returns default port if heroku-port isn't set (i.e. on localhost)
        if (System.getenv("PORT") != null) {
            port = Integer.parseInt(System.getenv("PORT"));
        }
        return port;
    }
}
