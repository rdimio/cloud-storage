package ru.geekbrains.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class FileController {

    private static final Logger log = Logger.getLogger(FileController.class);

    public synchronized static void sendFile(Path path, Channel channel) throws IOException {

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.SEND_FILE.getCode());
        log.info("Send command for file send");
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(path.getFileName().toString().length());
        log.info("Send Filename length");
        channel.writeAndFlush(buf);

        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        log.info("Send filename");
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        log.info("Send file size");
        channel.writeAndFlush(buf);

    }

    public synchronized static void receiveFile(ByteBuf buf, String url) throws IOException {
        int nextLength = 0;
        BufferedOutputStream out = null;
        if (buf.readableBytes() >= 4) {
            nextLength = buf.readInt();
            log.info("STATE: Get filename length");
        }
        if (buf.readableBytes() >= nextLength) {
            byte[] fileName = new byte[nextLength];
            buf.readBytes(fileName);
            log.info("STATE: Filename received - " + new String(fileName, "UTF-8"));
            String filenameString = new String(fileName, "UTF-8");
            File file = new File(url + "/" + filenameString);
            if (!file.exists()) {
                out = new BufferedOutputStream(new FileOutputStream(url + "/" + new String(filenameString)));
            }
        }
        long receivedFileLength = 0;
        long fileLength = 0;
        if (buf.readableBytes() >= 8) {
            fileLength = buf.readLong();
            log.info("STATE: File length received - " + fileLength);
        }
        while (buf.readableBytes() > 0) {
            if (out != null) {
                out.write(buf.readByte());
            }
            receivedFileLength++;
            if (fileLength == receivedFileLength) {
                log.info("File received");
                if (out != null) {
                    out.close();
                }
                break;
            }
        }

    }

    public synchronized static Path getFileByFileName(ByteBuf buf, String url) throws UnsupportedEncodingException {
        int nextLength = 0;

        if (buf.readableBytes() >= 4) {
            nextLength = buf.readInt();
            log.info("STATE: Get filename length");
        }
        if (buf.readableBytes() >= nextLength) {
            byte[] fileName = new byte[nextLength];
            buf.readBytes(fileName);
            log.info("STATE: Filename received - " + new String(fileName, "UTF-8"));
            String filenameString = new String(fileName, "UTF-8");
            return Paths.get(url + "/" + filenameString);
        } else return null;
    }

    public synchronized static void sendDelete(Channel channel, String fileName) {
        ByteBuf buf = null;

        log.info("Sending delete command for " + fileName);
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.DELETE.getCode());
        channel.writeAndFlush(buf);

        log.info("Sending file name length to delete");
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(fileName.length());
        channel.writeAndFlush(buf);

        log.info("Sending filename to delete...");
        byte[] filenameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);

    }

    public synchronized static void deleteFromStorage(ByteBuf buf, String url) throws IOException {
        int nextLength = 0;
        if (buf.readableBytes() >= 4) {
            log.info("STATE: Get filename to delete length");
            nextLength = buf.readInt();
        }
        if (buf.readableBytes() >= nextLength) {
            log.info("STATE: receiving file name to delete");
            byte[] fileName = new byte[nextLength];
            buf.readBytes(fileName);
            String filenameString = new String(fileName, "UTF-8");
            Files.deleteIfExists(Paths.get(url + "/" + filenameString));
            log.info("STATE: Filename received - " + filenameString);
        }
    }

    public synchronized static void sendFilesList(Channel channel, String url) throws IOException {
        ByteBuf buf;
        ArrayList<FileMessage> fl = new ArrayList<>();
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.SEND_FILE_LIST.getCode());
        channel.writeAndFlush(buf);
        log.info("Send refresh command");

        Path path = Paths.get(url);
        fl.clear();
        fl.addAll(Files.list(path).map(FileMessage::new).collect(Collectors.toList()));
        FileList fileList = new FileList(fl, url);
        byte[] bytes = filesToBytes(fileList);
        log.info("Got serialized list");

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(bytes.length);
        channel.writeAndFlush(buf);
        log.info("Send list length");

        buf = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buf.writeBytes(bytes);
        channel.writeAndFlush(buf);
        log.info("Sent file list");

    }

    public synchronized static FileList receiveFileList(ByteBuf buf, ByteBuf accum) throws InterruptedException {
        FileList fileList = new FileList();

        int nextLength = 0;
        if (buf.readableBytes() > 4) {
            nextLength = buf.readInt();
            log.info("Got file list length");
        }
        accum.writeBytes(buf);

        if (accum.readableBytes() >= nextLength) {
            byte[] fileBytes = new byte[nextLength];
            accum.readBytes(fileBytes);
            log.info("Got file list");
            fileList = FileController.filesFromBytes(fileBytes);
            accum.clear();
        }
        return fileList;
    }

    public synchronized static void sendRefresh(Channel channel) {
        ByteBuf buf = null;

        log.info("Sending refresh command");
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.SEND_FILE_LIST.getCode());
        channel.writeAndFlush(buf);

    }

    public synchronized static byte[] filesToBytes(FileList list) {
        log.info("Getting FileInfo list");
        byte[] bytes = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(baos);
            log.info("Serialize list");
            out.writeObject(list);
            bytes = baos.toByteArray();
            log.info("List serialized");
            out.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public synchronized static FileList filesFromBytes(byte[] bytes) {
        log.info("Getting bytes");
        FileList files = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream in = null;
        try {
            while (bais.available() > 0) {
                in = new ObjectInputStream(bais);
                log.info("Deserialize bytes");
                files = (FileList) in.readObject();
            }
            log.info("Bytes deserialized");
            if (in != null) {
                in.close();
            }
            bais.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return files;
    }

    public synchronized static void downloadFile(SocketChannel channel, String fileName) {
        ByteBuf buf = null;

        log.info("Sending download command for " + fileName);
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.RECEIVE_FILE.getCode());
        channel.writeAndFlush(buf);

        log.info("Sending file name length to download");
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(fileName.length());
        channel.writeAndFlush(buf);

        log.info("Sending filename to download...");
        byte[] filenameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);
    }
}
