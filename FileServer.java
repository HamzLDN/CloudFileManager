
import com.sun.net.httpserver.HttpServer;
import utils.Database;
import utils.UtilityClass;
import utils.ExpiredSessions.DeleteExpSessions;
import utils.ExpiredSessions.debug;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import utils.EndPointHandlers.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
public class FileServer {
    private static String[] MARIADB() {
        try {
            String stringToParse = new String(Files.readAllBytes(Paths.get("utils/credentials.json")), StandardCharsets.UTF_8);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(stringToParse);

            if (json.containsKey("MariaDb")) {
                JSONObject mariaDbObject = (JSONObject) json.get("MariaDb");
                String jdbcUrl = (String) mariaDbObject.get("JDBC_URL");
                String username = (String) mariaDbObject.get("USERNAME");
                String password = (String) mariaDbObject.get("PASSWORD");
                String[] array = {jdbcUrl,username,password};
                return array;
            } else {
                System.out.println("Key 'MariaDb' not found in the JSON file.");
            }
        } catch (IOException | org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setup_endpoints(Database databaseManager, int portnumber) throws IOException {
        String[] serverendpoints = {
            "/", 
            "/login", 
            "/signup",
            "/dashboard", 
            "/assets", 
            "/filemanager", 
            "/fetchdata", 
            "/recvdata", 
            "/senddata", 
            "/logout"
        };
        HttpServer server = HttpServer.create(new InetSocketAddress(portnumber), 1);
        DeleteExpSessions sessionCleanup = new DeleteExpSessions(databaseManager);
        sessionCleanup.start();
        server.createContext("/", new MyHandler(databaseManager, serverendpoints));
        server.createContext("/login", new Login(databaseManager));
        server.createContext("/signup", new SignUp(databaseManager));
        server.createContext("/dashboard", new Dashboard(databaseManager));
        server.createContext("/assets", new ResourceHandler());
        server.createContext("/filemanager", new filemanager(databaseManager));
        server.createContext("/fetchdata", new fetchdata(databaseManager));
        server.createContext("/upload", new upload(databaseManager));
        server.createContext("/recvdata", new recvdata(databaseManager));
        server.createContext("/deletecontent", new DeleteContent(databaseManager));
        server.createContext("/logout", new Logout(databaseManager));
        CompletableFuture.runAsync(() -> {
            server.setExecutor(null);
            server.start();
        });
    }
    public static void Initiate_Server(int portnumber) throws IOException{
        Database databaseManager = null;
        String[] values = MARIADB();
        
        try {
            databaseManager = new Database(values);
            setup_endpoints(databaseManager, portnumber);
            System.out.println("Server started on port " + portnumber + ". Press Ctrl+C to stop.");
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).join();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (databaseManager != null) {
                try {
                    databaseManager.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void main(String[] args) throws IOException, SQLException {
        int portnumber = 8000;
        try {
            portnumber = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.err.println(e);
        }
        Initiate_Server(portnumber);
    }

// Server RPS = 20 per second

static class ResourceHandler implements HttpHandler {
    // <IP,NUM_OF_REQUEST,TIMESTAMP>
    // Map<Integer, List<Object>> connections = new HashMap<>();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream os = exchange.getResponseBody();
        try {
            // connections.put(1, new ArrayList<>(Arrays.asList(strip_ip(exchange.getRemoteAddress()),  System.nanoTime())));
            
            // wafsecurity.scan_ips();
            String path = exchange.getRequestURI().getPath();
            String filename = "." + path;
            byte[] responseBytes = Files.readAllBytes(Paths.get(filename));
            debug.printlog(exchange, "[+] Loading content : " + filename);
            exchange.getResponseHeaders().set("Content-Type", getContentType(filename));
            exchange.sendResponseHeaders(200, responseBytes.length);
            os.write(responseBytes);
        } catch (IOException e) { System.err.println(e);}
        os.close();
        exchange.close();

    }
    private String strip_ip(InetSocketAddress ip) {
        String value = ip.toString();
        String[] address = value.split(":");
        return address[0];
    }
    private String getContentType(String filename) {
        if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".js")) {
            return "application/javascript";
        }
        return "text/plain";
    }
}

    static class SignUp implements HttpHandler {
        private final Database database;

        public SignUp(Database database) {
            this.database = database;
        }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                handlepost(exchange, database);
                debug.printlog(exchange, "Sign Up fetched a POST request.");
            } else if ("GET".equals(exchange.getRequestMethod())){
                servesignup(exchange, database);
                debug.printlog(exchange, "Sign Up fetched a GET reques.");
            }
            exchange.close();
        }
        private void servesignup(HttpExchange exchange, Database database) throws IOException {
            OutputStream os = exchange.getResponseBody();
            String response = UtilityClass.readFile("html/signup.html");
            exchange.sendResponseHeaders(200, response.length());
            os.write(response.getBytes());
            os.close();
        }

        private boolean checkUser(Database database, String user) {
            String sql_query = "SELECT username FROM users where username=?";
            String username = "";
            try {
                ResultSet resultSet = database.executeQuery(sql_query, user);
                while (resultSet.next()) {
                    username = resultSet.getString("username");
                    username = username.toLowerCase();
                    user = user.toLowerCase();
                    return username.equals(user);
                }
            } catch (SQLException e) {
                System.out.println("Error executing query: " + e.getMessage() + "\n" + sql_query);
            }
            return false;
        }

        private boolean create_directory(String folder) {
            folder = "UserFiles/" + folder;
            File directory = new File(folder);
            if(!directory.exists()) {
                boolean success = directory.mkdirs();
                return success;
            }
            return false;
        }
        private void handlepost(HttpExchange exchange, Database database) throws IOException {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder postData = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                postData.append(line);
            }
            Map<String, String> parameters = UtilityClass.parseFormData(postData.toString());
            String firstname = parameters.get("firstname");
            String lastname = parameters.get("lastname");
            String username = parameters.get("username");
            String password = UtilityClass.hashPassword(parameters.get("password"));
            if (!UtilityClass.IsValid(username)) {
                redirect(exchange, "InvalidCharacters",200);
            } else if (checkUser(database, username)) {
                redirect(exchange, "taken",200);
            } else {
                try {
                    update_user(exchange, firstname, lastname, username, password);
                } catch (SQLException e) {
                    redirect(exchange, "serverError", 200);
                } finally {
                    exchange.getResponseBody().close();
                }
            }
        }
        private void update_user(HttpExchange exchange, String firstname, String lastname, String username, String password) throws SQLException, IOException {
            String sql = "INSERT INTO users (firstname, lastname, username, password) VALUES (?,?,?,?)";
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            if (!create_directory(username)) {
                System.out.println("[-] Failed to make user directory for : " + username);

            } else {
                database.executeUpdate(sql, firstname, lastname, username, password);
                redirect(exchange, "success", 200);
                System.out.println("[+] Successfully created a user directory for : " + username);
            }
        }
        private static void redirect(HttpExchange exchange, String response, int status) throws IOException {
            OutputStream os = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, response.length());
            os.write(response.getBytes());
            os.close();
        }
    }
}
