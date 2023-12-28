package utils.EndPointHandlers;
import utils.Database;
import utils.UtilityClass;
import java.util.List;
import org.owasp.encoder.Encode;
import java.io.OutputStream;
import java.sql.SQLException;
import java.sql.ResultSet;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class Dashboard implements HttpHandler {
private final Database database;

public Dashboard(Database database) {
    this.database = database;
}

    private static String UserFileInfo(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            return Integer.toString(directory.list().length);
        }
        return "0";
    }
    private static long incrementsize(File file) {
        if (file.isFile()) {
            return file.length();
        } else if (file.isDirectory()) {
            return getFolderSize(file);
        }
        return 0;
    }
    private static long getFolderSize(File folder) {
        long size = 0;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                size += incrementsize(file);
            }
        }

        return size;
    }

    private static String foldersize(File folder) {
        return formatSize(getFolderSize(folder));
    }

    private static String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return size / 1024 + " KB";
        } else if (size < 1024 * 1024 * 1024) {
            return size / (1024 * 1024) + " MB";
        } else {
            return size / (1024 * 1024 * 1024) + " GB";
        }
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String username = "";
        String firstname = "";
        String lastname = "";
        if (!UtilityClass.handlesession(exchange, database)){
            exchange.getResponseHeaders().set("Location", "/");
            exchange.sendResponseHeaders(302, -1);
        }
        List<String> resultSet = UtilityClass.getuserinfo(exchange, database);
        Map<String, Object> variables = new HashMap<>();
        firstname = Encode.forHtml(resultSet.get(1));
        lastname = Encode.forHtml(resultSet.get(2));
        username = Encode.forHtml(resultSet.get(0));
        variables.put("firstname", firstname);
        variables.put("lastname", lastname);
        variables.put("username", username);
        variables.put("totalfiles",UserFileInfo("UserFiles/"+username));
        variables.put("totalfoldersize",foldersize(new File("UserFiles/"+username)));
        String htmlFilePath = "html/dashboard.html";
        String htmlContent = new String(Files.readAllBytes(Paths.get(htmlFilePath)), StandardCharsets.UTF_8);
        htmlContent = UtilityClass.render_html(htmlContent, variables);
        byte[] responseBytes = htmlContent.getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
