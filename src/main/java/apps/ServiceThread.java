package apps;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceThread implements Runnable {

    private Socket clientSocket;
    private static ConcurrentHashMap<String, Handler> urlHandler;
    private static BufferedReader in;
    private static PrintWriter out;
    private static BufferedOutputStream dataOut;

    
    public ServiceThread(Socket clientSocket, ConcurrentHashMap<String, Handler> urlHandler) {
        this.clientSocket = clientSocket;
        System.out.println("Connection accepted.");
        this.urlHandler = urlHandler;
    }

    public void run() {
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
                        req = req.substring(0, req.indexOf("?"));
                    } else {
                        req = req.substring(0, req.length() - 1);
                    }
                    if (urlHandler.containsKey(req)) {
                        // Content
                        String content = null;
                        try {
                            if (methodArgs.equals("")) {
                                content = (urlHandler.get(req)).process();
                            } else {
                                content = (urlHandler.get(req)).process(methodArgs);
                            }
                            // Header
                            HttpServer.headerResponse(out, null, "text/html", res);
                        } catch (HttpServerException ex) {
                            System.out.println(ex.getMessage());
                            String[] newHeader = new String[]{"GET", "/404.html", "HTTP/1.1"};
                            HttpServer.httpHandler(newHeader, out, dataOut);
                        }
                        if (content == null) {
                            String[] newHeader = new String[]{"GET", "/404.html", "HTTP/1.1"};
                            HttpServer.httpHandler(newHeader, out, dataOut);
                        } else {
                            out.write(content + "\r\n");
                            out.flush();
                        }
                    } else {
                        String[] newHeader = new String[]{"GET", "/404.html", "HTTP/1.1"};
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
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

}