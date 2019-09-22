package apps;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service class allows to read every class and method
 * that a developer creates and given a request be able
 * to response correctly.
 */
public class Service {

    private static ConcurrentHashMap<String, Handler> urlHandler = new ConcurrentHashMap();
    private static int port = getPort();
    private final static int nThreads = 100;
    private static ExecutorService pool;
    private static ServerSocket serverSocket;

    /**
     * When listen is execute, all the connections between server and client start
     * and stay ready to receive requests.
     */
    public static void listen() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening for connections on port --> " + port);
            //Create thread pool.
            pool = Executors.newFixedThreadPool(nThreads);
        } catch (IOException ex) {
            System.out.println("Could not listen on port: " + port + ". IOException: " + ex);
        }
        while (true) {
            // Keep connection until client disconnect to the server.
            try {
                Socket clientSocket = serverSocket.accept();
                //Create a new thread when a client connects.
                pool.execute(new ServiceThread(clientSocket, urlHandler));
            } catch (IOException e) {
                System.out.println("Could not accept the connection to client.");
                shutdownAndAwaitTermination(pool);
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
     * <p>
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

    /**
     * The following method shuts down an ExecutorService in two phases, first by calling shutdown
     * to reject incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering tasks.
     */
    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
