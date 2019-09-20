package apps;

public class HttpServerException extends Exception {

    public final static String NEED_PARAMS = "The method needs params to be execute.";
    public final static String INCORRECT_PARAMS = "The params sent are not correct. Verify them.";

    public HttpServerException(String message) {
        super(message);
    }
}
