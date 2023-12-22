package utils.EndPointHandlers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.Database;
import utils.UtilityClass;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.nio.charset.StandardCharsets;

public class Login implements HttpHandler {
    private final Database database;

    public Login(Database database) {
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Check if the request method is POST
        if (UtilityClass.handlesession(exchange, database)) {
            exchange.getResponseHeaders().set("Location", "/filemanager");
            exchange.sendResponseHeaders(302, -1);

        }
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                handlePostRequest(exchange, database);
            } catch (SQLException e) {
                System.err.println(e);
            }
        } else if ("GET".equals(exchange.getRequestMethod())){
            loginpage(exchange, 405);
        }
    }


    private String generateSessionString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            stringBuilder.append(randomChar);
        }

        return stringBuilder.toString();
    }

    public String generateUniqueSessionId(int length) throws SQLException {
        String sessionId = "";
        try {
            do {
                sessionId = generateSessionString(length);
            } while (!isSessionIdUnique(sessionId));
        } catch (SQLException e) {
            System.out.println("Error checking uniqueness of session id: " + e.getMessage());
        }
        return sessionId;
    }

    private boolean isSessionIdUnique(String sessionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sessions WHERE session_id = ?";
        ResultSet resultSet = database.executeQuery(sql, sessionId);
        resultSet.next();
        return resultSet.getInt(1) == 0;
    }


    private void handlePostRequest(HttpExchange exchange, Database database) throws IOException, SQLException {

        // Get the output stream to send the response
        OutputStream os = exchange.getResponseBody();
        // Get the input stream to read the POST data
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);

        // Read the POST data from the input stream
        StringBuilder postData = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            postData.append(line);
        }
        // Parse the POST data as URL-encoded parameters
        Map<String, String> parameters = UtilityClass.parseFormData(postData.toString());
        // Extract the username and password
        String username = parameters.get("username");
        String password = parameters.get("password");
        password = UtilityClass.hashPassword(password);
        // Generate session token and post to client
        boolean loginSuccessful = false;
        String user_id = "";
        ResultSet resultSet = database.executeQuery("SELECT * FROM users WHERE username = ?", username);
        
        while (resultSet.next()) {
            user_id = resultSet.getString("id");
            if (username.equals(resultSet.getString("username")) && password.equals(resultSet.getString("password"))) {
                // Successful login
                loginSuccessful = true;
                break;
            }
        }
        if (loginSuccessful) {
            loginSuccess(exchange, database, user_id);
        } else {
            exchange.getResponseHeaders().set("Location", "/login");
            exchange.sendResponseHeaders(302, -1);

        }
        exchange.close();
        os.close();
    }

    private void loginSuccess(HttpExchange exchange, Database database, String user_id) throws SQLException, IOException  {
        // Get the current timestamp
        long currentTimeMillis = System.currentTimeMillis();
        Timestamp currentTimestamp = new Timestamp(currentTimeMillis);

        // Create a Calendar and add 1 day to the current timestamp
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTimestamp);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        // Get the new timestamp after adding 1 day
        Timestamp expiryTimestamp = new Timestamp(calendar.getTimeInMillis());
        String expiryTimestampStr = expiryTimestamp.toString();
        String cookieValue = generateUniqueSessionId(128);
        exchange.getResponseHeaders().add("Set-Cookie", "SessionID=" + cookieValue);
        String sql = "INSERT INTO sessions (session_id, user_id, expiry_timestamp, user_agent) VALUES (?, ?, ?, ?)";
                
        List<String> userAgentHeaders = exchange.getRequestHeaders().get("User-Agent");
        // Execute the SQL query
        if (userAgentHeaders != null && !userAgentHeaders.isEmpty()) {
            String userAgent = userAgentHeaders.get(0);
            try {
                database.executeUpdate(sql, cookieValue, user_id, expiryTimestampStr, userAgent);
                // Process the result set as needed
            } catch (SQLException e) {
                e.printStackTrace(); // Handle the exception appropriately
            }
            // Set the response headers for a 302 Found (Temporary redirect)
            exchange.getResponseHeaders().set("Location", "/filemanager");
            exchange.sendResponseHeaders(302, -1);
        } else {
            exchange.getResponseHeaders().set("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
        }
    }

    private void loginpage(HttpExchange exchange, int statusCode) throws IOException {
        UtilityClass.handlesession(exchange, database);
        // Get the output stream to send the response
        OutputStream os = exchange.getResponseBody();

        // Specify the path to your HTML file
        String htmlFilePath = "html/login.html";

        // Read the HTML content from the file
        String response = UtilityClass.readFile(htmlFilePath);

        // Embed the CSS content directly in the HTML
        String cssContent = UtilityClass.readFile("static/style.css");
        response = response.replace("{{css_placeholder}}", "<style>" + cssContent + "</style>");

        // Send the response headers
        exchange.sendResponseHeaders(200, response.length());

        // Write the HTML content to the output stream
        os.write(response.getBytes());

        // Close the output stream and the exchange
        os.close();
        exchange.close();
    }
}