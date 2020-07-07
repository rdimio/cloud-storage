package ru.geekbrains.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.geekbrains.common.FileMessage;

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
    static int cnt = 0;
    private String userName;


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
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            while (in.isReadable()) {
                System.out.print((char) in.readByte());
                System.out.flush();
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws IOException {
        System.out.println("Client connected!");
        clients.add(ctx);
        cnt++;
        userName = "user#" + cnt;
        Path path = Paths.get("./server/src/main/resources/data");
        List<FileMessage> fl = Files.list(path).map(FileMessage::new).collect(Collectors.toList());
        ctx.writeAndFlush(fl);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clients.remove(ctx);
        cnt--;
    }

}
