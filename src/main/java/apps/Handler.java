package apps;

/**
 * Handler interface has all functions to handler
 * a request.
 */
public interface Handler {

    /**
     * Process allows to execute the method that
     * this handler is keeping.
     *
     * @param methodArg represent the params of the method to handle.
     * @return String : the response of the method.
     */
    String process(String methodArg);

    /**
     * Process allows to execute the method that
     * this handler is keeping.
     *
     * @return String : the response of the method.
     */
    String process();
}
