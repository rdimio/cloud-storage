package ru.geekbrains.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.common.CommandType;
import ru.geekbrains.common.FileController;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedDeque;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    private NettyServer server;
    static ConcurrentLinkedDeque<ChannelHandlerContext> clients = new ConcurrentLinkedDeque<>();
    private static final String STORAGE = "./server/src/main/resources/data";
    private static final Logger log = Logger.getLogger(NettyServerHandler.class);

    public NettyServerHandler() {
        server = new NettyServer();
    }

    public void serverStart() {
        new Thread(server).start();
    }

    public void serverStop() {
        server.disconnect();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        if (msg == null) return;
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            byte read = buf.readByte();
            if(read == CommandType.SEND_FILE.getCode()) {
                FileController.receiveFile(buf, STORAGE);
            }
            if(read == CommandType.RECEIVE_FILE.getCode()) {
                Path path = FileController.getFileByFileName(buf, STORAGE);
                FileController.sendFile(path, ctx.channel());
            }
            if(read == CommandType.SEND_FILE_LIST.getCode()) {
                FileController.sendFilesList(ctx.channel(), STORAGE);
            }
            if(read == CommandType.DELETE.getCode()) {
                FileController.deleteFromStorage(buf, STORAGE);
            }
        }
        buf.release();
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Client connected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("connection close");
    }
}
