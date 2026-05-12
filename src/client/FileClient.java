package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

/**
 * The client application for the Remote File Manager.
 */
public class FileClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final String DOWNLOADS_DIR = "downloads";

    public static void main(String[] args) {
        startUdpListener();

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            boolean running = true;
            while (running) {
                printMenu();
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        handleList(output, input);
                        break;
                    case "2":
                        handleUpload(scanner, output, input);
                        break;
                    case "3":
                        handleDownload(scanner, output, input);
                        break;
                    case "4":
                        handleDelete(scanner, output, input);
                        break;
                    case "5":
                        output.println("EXIT");
                        running = false;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    /**
     * Starts a background thread to listen for UDP notifications on port 5001.
     */
    private static void startUdpListener() {
        Thread udpListener = new Thread(() -> {
            try {
                MulticastSocket multicastSocket = new MulticastSocket(5001);
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("\n" + message);
                }
            } catch (Exception e) {
                System.out.println("UDP Listener error: " + e.getMessage());
            }
        });
        udpListener.setDaemon(true);
        udpListener.start();
    }

    private static void printMenu() {
        System.out.println("\n=== Remote File Manager ===");
        System.out.println("[1] List Files");
        System.out.println("[2] Upload File");
        System.out.println("[3] Download File");
        System.out.println("[4] Delete File");
        System.out.println("[5] Exit");
        System.out.print("Enter choice: ");
    }

    private static void handleList(PrintWriter output, BufferedReader input) throws IOException {
        output.println("LIST");
        String response;
        System.out.println("\n--- Server Files ---");
        while ((response = input.readLine()) != null) {
            if ("END".equals(response)) {
                break;
            }
            System.out.println(response);
        }
        System.out.println("--------------------");
    }

    private static void handleUpload(Scanner scanner, PrintWriter output, BufferedReader input) {
        System.out.print("Enter local file path to upload: ");
        String filePath = scanner.nextLine().trim().replace("\"", "");
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.out.println("Error: File does not exist or is not a valid file.");
            return;
        }

        output.println("UPLOAD " + file.getName());

        try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                output.println(line);
            }
            output.println("END");
            System.out.println("Upload request sent.");
            
            // Read server response
            String response = input.readLine();
            if (response != null) {
                System.out.println("Server: " + response);
            }
        } catch (IOException e) {
            System.err.println("Error reading local file: " + e.getMessage());
            output.println("END"); // To stop server from waiting forever
        }
    }

    // Changing signature to include input
    private static void handleDownload(Scanner scanner, PrintWriter output, BufferedReader input) throws IOException {
        System.out.print("Enter filename to download: ");
        String filename = scanner.nextLine();
        output.println("DOWNLOAD " + filename);

        String firstLine = input.readLine();
        if (firstLine != null && firstLine.startsWith("Error:")) {
            System.out.println(firstLine);
            // read END
            input.readLine();
            return;
        }

        File dir = new File(DOWNLOADS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, filename);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // firstLine is part of the file content
            if (firstLine != null && !"END".equals(firstLine)) {
                writer.write(firstLine);
                writer.newLine();
            } else if ("END".equals(firstLine)) {
                System.out.println("File is empty.");
                return;
            }

            String line;
            while ((line = input.readLine()) != null) {
                if ("END".equals(line)) {
                    break;
                }
                writer.write(line);
                writer.newLine();
            }
            System.out.println("File downloaded successfully to '" + DOWNLOADS_DIR + "' directory.");
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }

    private static void handleDelete(Scanner scanner, PrintWriter output, BufferedReader input) throws IOException {
        System.out.print("Enter filename to delete: ");
        String filename = scanner.nextLine();
        output.println("DELETE " + filename);
        
        String response = input.readLine();
        if (response != null) {
            System.out.println(response);
        }
    }
}
