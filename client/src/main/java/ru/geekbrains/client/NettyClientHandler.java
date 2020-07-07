package ru.geekbrains.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.common.FileList;


public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private NettyClient nettyClient;
    private static FileList fl;

    public NettyClientHandler(int port, String host) {
       nettyClient = new NettyClient(port, host);
    }

    public void connectServer() {
        new Thread(nettyClient).start();
    }

    public void disconnectServer() {
        nettyClient.disconnect();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if(msg instanceof FileList) {
            fl = (FileList) msg;
            System.out.println(fl);
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public FileList getFl() {
        return fl;
    }

}
