package apps;

/**
 * Web Service allows to developers create new functions for their
 * web application.
 */
public class WebService {

    @Web("square")
    public static String square(String number) {
        int param = Integer.parseInt(number);
        return "<html>" +
                "<head>" +
                "<title>Result</title>" +
                "</head>" +
                "<body>" +
                "<h1>Inicio</h1>" +
                "<div>El cuadrado de " + number + " es: </div>" +
                "<div>" + param * param + "</div>" +
                "<div><a href='/'>Volver</a></div>" +
                "</body>" +
                "</html>";
    }

    @Web("hello")
    public static String hello() {
        return "<html>" +
                "<head>" +
                "<title>Result</title>" +
                "</head>" +
                "<body>" +
                "<h1>Hello</h1>" +
                "<div>Esta página no recibe un parámetro</div>" +
                "</body>" +
                "</html>";
    }
}
