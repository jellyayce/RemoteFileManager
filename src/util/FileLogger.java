package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


//Utility class for logging activity.
public class FileLogger {

    private BufferedWriter writer;
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + File.separator + "activity.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    //Constructs a new FileLogger. Initializes the log file and directory.
    public FileLogger() {
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // Append mode is true
            writer = new BufferedWriter(new FileWriter(LOG_FILE, true));
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    /**
     * Logs a message with a timestamp.
     *
     * @param message the message to log
     */
    public void log(String message) {
        if (writer != null) {
            try {
                String timestamp = LocalDateTime.now().format(FORMATTER);
                writer.write(String.format("[%s] %s", timestamp, message));
                writer.newLine();
                writer.flush(); // ensure it's written immediately
            } catch (IOException e) {
                System.err.println("Failed to write log: " + e.getMessage());
            }
        }
    }

    
    //Closes the logger.
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                System.err.println("Failed to close logger: " + e.getMessage());
            }
        }
    }
}
