package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executors;

/**
 * An HTTP server that serves files from the server_files directory.
 */
public class HttpFileServer implements Runnable {

    private static final int PORT = 8080;
    private static final String SERVER_FILES_DIR = "server_files";

    /**
     * Starts the HTTP server on port 8080.
     */
    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new RootHandler());
            server.createContext("/files/", new FileHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (IOException e) {
            System.err.println("HTTP Server error: " + e.getMessage());
        }
    }

    /**
     * Handles requests to the root path (/) by listing available files.
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html><body>\n");
            htmlBuilder.append("<pre>\n");
            htmlBuilder.append("=== NetDrive File Server ===\n");
            htmlBuilder.append("Available Files:\n");

            File dir = new File(SERVER_FILES_DIR);
            File[] files = dir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String filename = file.getName();
                        String link = "http://localhost:8080/files/" + filename;
                        htmlBuilder.append(String.format("- %-15s <a href=\"%s\">%s</a>\n", filename, link, link));
                    }
                }
            }

            htmlBuilder.append("</pre>\n");
            htmlBuilder.append("</body></html>\n");

            String response = htmlBuilder.toString();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] responseBytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    /**
     * Handles file download requests (/files/filename).
     */
    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String filename = path.substring("/files/".length());
            File file = new File(SERVER_FILES_DIR, filename);

            if (!file.exists() || !file.isFile()) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String contentType = filename.endsWith(".txt") ? "text/plain" : "application/octet-stream";
            exchange.getResponseHeaders().set("Content-Type", contentType);
            
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }
}
