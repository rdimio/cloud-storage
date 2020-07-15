package ru.geekbrains.common;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileMessage implements Serializable {

    public enum FileType {
        FILE("F"), DIRECTORY("D");
        private String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    public String getFilename() {
        return filename;
    }
    public FileType getType() {
        return type;
    }
    public long getSize() {
        return size;
    }
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    public byte[] getFileByteArray() { return fileByteArray; }

    private String filename;
    private FileType type;
    private long size;
    private LocalDateTime lastModified;
    private byte[] fileByteArray;

    public FileMessage(Path path) {
        try {
            this.size = Files.size(path);
            this.filename = path.getFileName().toString();
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY: FileType.FILE;
            if(this.type == FileType.DIRECTORY) {
                this.size = -1L;
            }
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
            fileByteArray = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create file info from path");
        }
    }

}
