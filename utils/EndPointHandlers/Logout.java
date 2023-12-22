package utils.EndPointHandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.Database;
import utils.UtilityClass;
import java.io.IOException;

public class Logout implements HttpHandler {
    private final Database database;

    public Logout(Database database) {
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Uncomment the following line to set a cookie to invalidate the session
        exchange.getResponseHeaders().add("Set-Cookie", "SessionID=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");

        String sessionID = UtilityClass.getSessionIDCookieValue(exchange, "SessionID");

        try (java.sql.ResultSet db = database.executeQuery("delete from sessions WHERE session_id = ?", sessionID)) {
            System.out.println("Deleted -> " + sessionID);
        } catch (Exception e) {
            System.err.println(e);
        }

        // Redirect to the home page or login page after logout
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
