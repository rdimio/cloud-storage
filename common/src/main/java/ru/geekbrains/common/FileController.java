package ru.geekbrains.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class FileController {

    public static void sendFile(Path path, Channel channel) throws IOException {

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.SEND_FILE.getCode());
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(path.getFileName().toString().length());
        channel.writeAndFlush(buf);

        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf);

    }

    public static void receiveFile(ByteBuf buf, String url) throws IOException {
        int nextLength = 0;
        BufferedOutputStream out = null;
        if (buf.readableBytes() >= 4) {
            nextLength = buf.readInt();
        }
        if (buf.readableBytes() >= nextLength) {
            byte[] fileName = new byte[nextLength];
            buf.readBytes(fileName);
            String filenameString = new String(fileName, "UTF-8");
            File file = new File(url + "/" + filenameString);
            if (!file.exists()) {
                out = new BufferedOutputStream(new FileOutputStream(url + "/" + new String(filenameString)));
            }
        }
        if (buf.readableBytes() >= 8) {
            long fileLength = buf.readLong();
            long receivedFileLength = 0;
            while (buf.readableBytes() > 0) {
                if (out != null) {
                    out.write(buf.readByte());
                }
                receivedFileLength++;
                if (fileLength == receivedFileLength) {
                    if (out != null) {
                        out.close();
                    }
                    break;
                }
            }
        }
    }

    public static void downloadFile(Channel channel, String fileName) {
        ByteBuf buf = null;

        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.RECEIVE_FILE.getCode());
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(fileName.length());
        channel.writeAndFlush(buf);

        byte[] filenameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);
    }

    public static Path getFileByFileName(ByteBuf buf, String url) throws UnsupportedEncodingException {
        int nextLength = buf.readInt();
        byte[] fileName = new byte[nextLength];
        buf.readBytes(fileName);
        String filenameString = new String(fileName, "UTF-8");
        return Paths.get(url + "/" + filenameString);
    }

    public static void sendDelete(Channel channel, String fileName) {
        ByteBuf buf = null;

        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.DELETE.getCode());
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(fileName.length());
        channel.writeAndFlush(buf);

        byte[] filenameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);

    }

    public static void deleteFromStorage(ByteBuf buf, String url) throws IOException {
        int nextLength = 0;
        if (buf.readableBytes() >= 4) {
            nextLength = buf.readInt();
        }
        if (buf.readableBytes() >= nextLength) {
            byte[] fileName = new byte[nextLength];
            buf.readBytes(fileName);
            String filenameString = new String(fileName, "UTF-8");
            Files.deleteIfExists(Paths.get(url + "/" + filenameString));
        }
    }

    public static void sendFilesList(Channel channel, String url) throws IOException {
        ByteBuf buf;
        ArrayList<FileMessage> fl = new ArrayList<>();
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.SEND_FILE_LIST.getCode());
        channel.writeAndFlush(buf);

        Path path = Paths.get(url  );
        fl.clear();
        fl.addAll(Files.list(path).map(FileMessage::new).collect(Collectors.toList()));
        FileList fileList = new FileList(fl, url);
        byte[] bytes = filesToBytes(fileList);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(bytes.length);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buf.writeBytes(bytes);
        channel.writeAndFlush(buf);

    }

    public static FileList receiveFileList(ByteBuf buf, ByteBuf accum) throws InterruptedException {
        FileList fileList = new FileList();

        int nextLength = 0;
        if (buf.readableBytes() > 4) {
            nextLength = buf.readInt();
        }
        accum.writeBytes(buf);

        if (accum.readableBytes() >= nextLength) {
            byte[] fileBytes = new byte[nextLength];
            accum.readBytes(fileBytes);
            fileList = FileController.filesFromBytes(fileBytes);
            accum.clear();
        }
        return fileList;
    }

    public static void sendRefresh(Channel channel) {
        ByteBuf buf = null;

        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(CommandType.SEND_FILE_LIST.getCode());
        channel.writeAndFlush(buf);

    }

    public static byte[] filesToBytes(FileList list) {
        byte[] bytes = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(baos);
            out.writeObject(list);
            bytes = baos.toByteArray();
            out.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static FileList filesFromBytes(byte[] bytes) {
        FileList files = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream in = null;
        try {
            while (bais.available() > 0) {
                in = new ObjectInputStream(bais);
                files = (FileList) in.readObject();
            }
            if (in != null) {
                in.close();
            }
            bais.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return files;
    }
}
