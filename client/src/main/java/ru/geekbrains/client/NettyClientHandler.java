package ru.geekbrains.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import ru.geekbrains.common.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private final NettyClient nettyClient;
    private static FileList list;
    private static String url;
    private static boolean listRefreshed;
    private static final Logger log = Logger.getLogger(NettyClientHandler.class);
    private CountDownLatch clientStarter;
    private final String host;
    private final int port;

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException {
        if (msg == null) return;
        ByteBuf buf = ((ByteBuf) msg);
//        System.out.println(buf.readableBytes());
        byte read = buf.readByte();
//        System.out.println(buf.readableBytes());

        if (read == CommandType.SEND_FILE_LIST.getCode()) {
            System.out.println(buf.readableBytes());
            ByteBuf accum = ctx.alloc().directBuffer(1024 * 1024, 10 * 1024 * 1024);
            listRefreshed = false;
            list = FileController.receiveFileList(buf, accum);
            if (list != null) {
                listRefreshed = true;
                log.info("files list is received");
            }
        }
        if (read == CommandType.SEND_FILE.getCode()) {
            FileController.receiveFile(buf, url);
        }

        if (buf.readableBytes() == 0) {
            buf.release();
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

    public void setUrl(String url) {
        NettyClientHandler.url = url;
    }

    public static boolean isListRefreshed() {
        return listRefreshed;
    }
}
