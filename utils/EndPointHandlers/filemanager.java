package utils.EndPointHandlers;
import utils.ExpiredSessions.debug;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.io.IOException;
import utils.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class filemanager implements HttpHandler {
    private final Database database;

    public filemanager(Database database) {
        this.database = database;
    }


    private String navigateuser(HttpExchange exchange, String endpoint) {

        Path path = FileSystems.getDefault().getPath(endpoint);
        if (Files.isDirectory(path)) {
            return "dir";
        } else if (Files.exists(path)) {
            return "file";
        } else {
            return "";
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, Object> variables = new HashMap<>();
        String response = UtilityClass.readFile("html/filemanager.html");
        OutputStream os = exchange.getResponseBody();
        URI uri = exchange.getRequestURI();
        String endpoint = uri.getPath();
        String[] parts = Arrays.stream(endpoint.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        debug.printlog(exchange, "[+] Attempting to handle session " + endpoint);
        if (!UtilityClass.handlesession(exchange, database)) {
            debug.printlog(exchange, "[-] Not a valid session. REDIRECTING BACK TO '/'");
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        } else {

            List<String> userInfo = UtilityClass.getuserinfo(exchange, database);
            String username = userInfo.get(0);
            String firstname = userInfo.get(1);
            String lastname = userInfo.get(2);
            String perms = "";
            if (parts.length == 1) {
                exchange.getResponseHeaders().set("Location", "/filemanager/"+username);
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            if (parts.length >= 2) {
                System.out.println("PART1 ->" + parts[1]);
                if (username.equals(parts[1])) {
                    perms = "1";
                } else {
                    perms = "0";
                }
            }
            if (perms.equals("1")) {
                variables.put("username", username);
                variables.put("firstname", firstname);
                variables.put("lastname", lastname);
                variables.put("place1", "Your files");
                variables.put("place1-link", "/filemanager/" + username);
                variables.put("place2", "Dashboard");
                variables.put("place2-link", "/dashboard");
                variables.put("perms", perms);
            } else {
                exchange.getResponseHeaders().set("Location", "/filemanager/" + username);
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            }


            if (parts.length != 1) {
                System.out.println("JOINGING PARTS");
                String[] modifiedParts = new String[parts.length - 2];
                System.arraycopy(parts, 2, modifiedParts, 0, modifiedParts.length);
                String filepath = "UserFiles/" + parts[1] + "/" + String.join("/", modifiedParts);
                System.out.println(filepath);
                boolean directory = (navigateuser(exchange, filepath).equals("dir"));
                if (directory || (navigateuser(exchange, filepath).equals("file"))) {
                    String render_html = UtilityClass.render_html(response, variables);
                    byte[] responseBytes = render_html.getBytes();
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    os.write(responseBytes);
                }

            } else {
                String render_html = UtilityClass.render_html(response, variables);
                byte[] responseBytes = render_html.getBytes();
                if (navigateuser(exchange, "UserFiles").equals("dir") || (navigateuser(exchange, "UserFiles").equals("file"))) {
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    os.write(responseBytes);
                }
            }
            os.close();
            exchange.close();
        }
        }
    }