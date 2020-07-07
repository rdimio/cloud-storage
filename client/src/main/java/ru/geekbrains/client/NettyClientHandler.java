package ru.geekbrains.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.common.FileMessage;

import java.util.ArrayList;
import java.util.List;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private NettyClient nettyClient;
    private static List<FileMessage> fl;

    public NettyClientHandler(int port, String host) {
       nettyClient = new NettyClient(port, host);
       fl = new ArrayList<>();
    }

    public void connectServer() {
        new Thread(nettyClient).start();
    }

    public void disconnectServer() {
        nettyClient.disconnect();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        fl = (List<FileMessage>) msg;
        System.out.println(fl);
        setFl(fl);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public List<FileMessage> getFl() {
        return fl;
    }

    public void setFl(List<FileMessage> fl) {
        this.fl = fl;
    }
}
