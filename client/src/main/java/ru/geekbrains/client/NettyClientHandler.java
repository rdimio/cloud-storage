package ru.geekbrains.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import ru.geekbrains.common.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private final NettyClient nettyClient;
    private static FileList list;

    private final Logger log = Logger.getLogger(NettyClientHandler.class);
    private final CountDownLatch clientStarter;
    private final String host;
    private final int port;
    private static final String url = "./client/src/main/resources/data";

    public NettyClientHandler(int port, String host) {
        this.host = host;
        this.port = port;
        clientStarter = new CountDownLatch(1);
        nettyClient = NettyClient.getInstance();
    }

    public void connectServer() throws InterruptedException {
        nettyClient.connect(clientStarter, host, port);
        clientStarter.await();
    }

    public void disconnectServer() {
        nettyClient.disconnect();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, ClassNotFoundException {
        if (msg == null) return;

        if (msg instanceof FileList) {
            list = (FileList) msg;
            log.info("FileList is received " + list);
        }
        if(msg instanceof File) {
            File file = (File) msg;
            log.info(file + " is received");
            FileController.copyFileUsingStream(file, new File(url + "/" + file.getName()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public NettyClient getNettyClient() {
        return nettyClient;
    }

    public static FileList getList() {
        return list;
    }

}
