package apps;

import java.io.IOException;

/**
 * Controller class allows to init the web services.
 */
public class Controller {

    private static Service service = new Service();

    public static void main(String[] args) throws IOException {
        Service serviceWebServer = getService();
        serviceWebServer.init();
        serviceWebServer.listen();
    }

    /**
     * Get the service of the session.
     *
     * @return Service : the service of the session.
     */
    public static Service getService() {
        return service;
    }
}
