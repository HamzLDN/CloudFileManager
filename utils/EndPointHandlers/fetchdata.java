package utils.EndPointHandlers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.Database;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Paths;
import java.nio.file.Path;
import utils.UtilityClass;
import java.util.Map;
import java.net.URI;
import java.util.Arrays;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.owasp.encoder.Encode;
import java.nio.file.DirectoryStream;
import java.util.stream.StreamSupport;
import java.util.Base64;


public class fetchdata implements HttpHandler {
    private final Database database;

    public fetchdata(Database database) {
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
    private static boolean verifyuser(HttpExchange exchange, Database database, String username) {
        List<String> user;
        try {
            user = UtilityClass.getuserinfo(exchange, database);
        } catch (Exception e) {
            return false;
        }
        if (!user.get(0).equals(username)) {
            return false;
        }
        return true;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String endpoint = uri.getPath();
        String[] parts = Arrays.stream(endpoint.split("/"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        System.out.println(parts.length);
        if (parts.length == 1) {
            exchange.close();
        }
        parts[0] = "UserFiles";
        String username = parts[1];
        String filepath =  String.join("/", parts);
        List<String> fileList = null;
        String fileListJson = null;
        if (!verifyuser(exchange,database,username)){
            exchange.close();
        }
        if (navigateuser(exchange, filepath).equals("dir")) {
            fileList = listFilesAndDirectories(filepath);
            if (fileList.size() == 0) {
                fileListJson = send_json("error", "");
            } else {
                fileListJson = convertListToJson(fileList);
            }
            
        } else if (navigateuser(exchange, filepath).equals("file")) {
            String filecontent;
            if (filepath.endsWith(".png")) {
                byte[] contentbytes = Files.readAllBytes(Paths.get(filepath));
                filecontent = encodeToBase64(contentbytes);
            } else {
                filecontent = new String(Files.readAllBytes(Paths.get(filepath)), StandardCharsets.UTF_8);
            }
            System.out.println("Encoding");
            byte[] data = send_json("content", Encode.forHtml(filecontent)).getBytes();
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Enable CORS
            try (OutputStream os = exchange.getResponseBody()) {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseHeaders().set("Transfer-Encoding", "chunked");

                int chunkSize = 1024;
                for (int i = 0; i < data.length; i += chunkSize) {
                    int length = Math.min(chunkSize, data.length - i);
                    os.write(data, i, length);
                    os.flush();
                }
            } catch (IOException e) {
                System.err.println("An error occurred while trying to write the response: " + e);
            }
            return;
        } else {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String json_data = send_json("status", "Error! Cannot get the file contents");
            exchange.sendResponseHeaders(200, json_data.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json_data.getBytes());
                os.close();
            }
            
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, fileListJson.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileListJson.getBytes());
        }

    }

    public static String encodeToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String send_json(String key, String file_content) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, file_content);
        jsonArray.add(jsonObject);
        return jsonArray.toJSONString();
    }

    public static String convertListToJson(List<String> list) {
        JSONArray jsonArray = new JSONArray();
        for (String item : list) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("file", item);
            jsonObject.put("timestamp", System.currentTimeMillis());
            jsonArray.add(jsonObject);
        }

        return jsonArray.toJSONString();
    }
    private List<String> listFilesAndDirectories(String directoryPath) throws IOException {
        Path directory = FileSystems.getDefault().getPath(directoryPath);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            return StreamSupport.stream(directoryStream.spliterator(), false)
                    .filter(path -> {
                        try {
                            return !Files.isHidden(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

}
