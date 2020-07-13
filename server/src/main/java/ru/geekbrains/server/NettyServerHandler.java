package ru.geekbrains.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import ru.geekbrains.common.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private NettyServer server;
    static ConcurrentLinkedDeque<ChannelHandlerContext> clients = new ConcurrentLinkedDeque<>();
    private static final String STORAGE = "./server/src/main/resources/data";
    private static final Logger log = Logger.getLogger(NettyServerHandler.class);
    private String fileName;
    private File file;

    public NettyServerHandler() {
        server = new NettyServer();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        if (msg == null) return;
        if(msg instanceof String)
            fileName = (String) msg;
        if(msg instanceof File) {
            file = (File) msg;
            log.info(file + " is received");
        }
        if(msg instanceof State) {
            State state = (State) msg;

            if (state == State.LIST_REQUEST) {
                log.info("server sends FileList");
                Path path = Paths.get(STORAGE);
                List<FileMessage> fl = Files.list(path).map(FileMessage::new).collect(Collectors.toList());
                FileList fileList = new FileList(fl, STORAGE);
                ctx.writeAndFlush(fileList);
            }
            if (state == State.FILE_DOWNLOAD) {
                log.info("server sends file " + fileName);
                File file = new File(STORAGE + "/" + fileName);
                ctx.writeAndFlush(file);
            }
            if(state == State.FILE_DELETE) {
                log.info("delete file on server: " + fileName);
                Files.delete(Paths.get(STORAGE + "/" + fileName));
            }
            if(state == State.FILE_RECEIVE) {
                log.info("received file on server " + fileName);
                FileController.copyFileUsingStream(file, new File(STORAGE + "/" + file.getName()));
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
