package utils.EndPointHandlers;
import utils.Database;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.io.IOException;
import utils.*;
import java.net.URI;

public class MyHandler implements HttpHandler {
    private final Database database;
    private final String[] endpoints;

    public MyHandler(Database database, String[] endpoints) {
        this.database = database;
        this.endpoints = endpoints;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String userendpoint = uri.getPath();
        boolean valid = false;
        for (int i = 0; i < endpoints.length; i++) {
            if (userendpoint.equals(endpoints[i])) {
                valid = true;
            } else {continue;}
        }
        if (valid) {
            OutputStream os = exchange.getResponseBody();
            String response = UtilityClass.readFile("html/index.html");
            exchange.sendResponseHeaders(200, response.length());
            os.write(response.getBytes());
            os.close();
            exchange.close();
        } else {
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
        }

    }
}