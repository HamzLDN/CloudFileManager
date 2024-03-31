package utils.EndPointHandlers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import utils.Database;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.OutputStream;
import java.util.Arrays;
import utils.UtilityClass;
import java.util.List;

public class recvdata implements HttpHandler {
    private final Database database;

    public recvdata(Database database) {
        this.database = database;
    }
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream os = exchange.getResponseBody();
        URI uri = exchange.getRequestURI();
        String endpoint = uri.getPath();
        String[] parts = Arrays.stream(endpoint.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        parts[0] = "UserFiles";
        String filepath = String.join("/", parts);
        List<String> user;
        String username;
        try {
            username = parts[1];
            user = UtilityClass.getuserinfo(exchange, database);
        } catch (Exception e) {
            os.close();
            exchange.close();
            return;
        }
        if (!user.get(0).equals(username)) {
            os.close();
            exchange.close();
        }
        byte[] content = Files.readAllBytes(Paths.get(filepath));
        exchange.sendResponseHeaders(200, content.length);
        os.write(content);
        os.close();
        exchange.close();
    }
}
    