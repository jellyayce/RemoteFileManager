package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

interface Uploadable {
    void upload(String destinationPath);
}

interface Downloadable {
    void download(String sourcePath);
}

public abstract class FileItem {
    private String fileName;
    private long fileSize;

    public FileItem(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public abstract String getInfo();
    public abstract String getType();

    @Override
    public String toString() {
        return "[" + getType() + "] " + fileName + " (" + fileSize + " bytes)";
    }
}

class TextFile extends FileItem implements Uploadable, Downloadable {
    private String encoding = "UTF-8";

    public TextFile(String fileName, long fileSize) {
        super(fileName, fileSize);
    }

    public TextFile(String fileName, long fileSize, String encoding) {
        super(fileName, fileSize);
        this.encoding = encoding;
    }

    @Override
    public String getType() {
        return "TEXT";
    }

    @Override
    public String getInfo() {
        return String.format("Filename: %s, Size: %d bytes, Encoding: %s", getFileName(), getFileSize(), encoding);
    }

    @Override
    public void upload(String destinationPath) {
        File source = new File(getFileName());
        File destFolder = new File(destinationPath);
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }
        File dest = new File(destFolder, source.getName());
        try (BufferedReader reader = new BufferedReader(new FileReader(source));
             BufferedWriter writer = new BufferedWriter(new FileWriter(dest))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Text file uploaded successfully to " + dest.getPath());
        } catch (IOException e) {
            System.err.println("Error uploading text file: " + e.getMessage());
        }
    }

    @Override
    public void download(String sourcePath) {
        File sourceFolder = new File(sourcePath);
        File source = new File(sourceFolder, getFileName());
        File dest = new File(getFileName());
        try (BufferedReader reader = new BufferedReader(new FileReader(source));
             BufferedWriter writer = new BufferedWriter(new FileWriter(dest))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Text file downloaded successfully to " + dest.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error downloading text file: " + e.getMessage());
        }
    }
}

class BinaryFile extends FileItem implements Uploadable, Downloadable {
    private String mimeType;

    public BinaryFile(String fileName, long fileSize, String mimeType) {
        super(fileName, fileSize);
        this.mimeType = mimeType;
    }

    @Override
    public String getType() {
        return "BINARY";
    }

    @Override
    public String getInfo() {
        return String.format("Filename: %s, Size: %d bytes, MIME Type: %s", getFileName(), getFileSize(), mimeType);
    }

    @Override
    public void upload(String destinationPath) {
        File source = new File(getFileName());
        File destFolder = new File(destinationPath);
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }
        File dest = new File(destFolder, source.getName());
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("Binary file uploaded successfully to " + dest.getPath());
        } catch (IOException e) {
            System.err.println("Error uploading binary file: " + e.getMessage());
        }
    }

    @Override
    public void download(String sourcePath) {
        File sourceFolder = new File(sourcePath);
        File source = new File(sourceFolder, getFileName());
        File dest = new File(getFileName());
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("Binary file downloaded successfully to " + dest.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error downloading binary file: " + e.getMessage());
        }
    }
}
