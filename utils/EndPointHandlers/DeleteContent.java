package utils.EndPointHandlers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import utils.Database;
import java.util.List;
import utils.UtilityClass;
import java.util.Arrays;

public class DeleteContent implements HttpHandler {
    private final Database database;

    public DeleteContent(Database database) {
        this.database = database;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException{
        List<String> userinfo = UtilityClass.getuserinfo(exchange, database);
        if (userinfo == null){
            return;
        }
        URI uri = exchange.getRequestURI();
        String endpoint = uri.getPath();
        String[] parts = Arrays.stream(endpoint.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        parts[0] = "UserFiles";
        if (parts.length == 1) {
            exchange.getResponseHeaders().set("Location", "/dashboard");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }
        
        String username = parts[1];
        System.out.println("delete from -> " + username);
        int removefile = remove_file(exchange, username, parts, userinfo);
        if (removefile == 1) {
            String response = "Successfully deleted file";
            send_outputstream(exchange, response);
        } else if (removefile == 0){
            String response = "Failed to delete file";
            send_outputstream(exchange, response);
        } else if (removefile == 2) {
            String response = "User information do not match";
            send_outputstream(exchange, response);
        }
        exchange.close();
    }
    private static int remove_file(HttpExchange exchange, String username, String[] parts, List<String> userinfo) throws IOException {
        // 1 is true, 0 is false, 2 is error matching user
        if (username.equals(userinfo.get(0))) {
            String filepath = String.join("/", parts);
            Path directory = Paths.get(filepath);
            System.out.println("delete -> " + filepath);
            if (validatePath(filepath)) {
                Files.delete(directory);
                return 1;
            } else {
                return 0;
            }
        }
        return 2;
    }
    private static void send_outputstream(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
            os.close();
        }
    }
    public static boolean validatePath(String filePathString) {
        // Convert the file path string to a Path object
        Path filePath = Paths.get(filePathString);

        try {
            // Check if the file path exists
            boolean exists = Files.exists(filePath);

            if (exists) {
                // Check if it's a regular file
                return Files.isRegularFile(filePath) || Files.isDirectory(filePath);
            } else {
                return false; // File path does not exist
            }
        } catch (Exception e) {
            return false; // An error occurred while validating the file path
        }
    }
}