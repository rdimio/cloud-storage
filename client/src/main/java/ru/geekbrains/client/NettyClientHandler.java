package ru.geekbrains.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;
import ru.geekbrains.common.*;
import ru.geekbrains.server.NettyServerHandler;

import java.io.IOException;
import java.nio.file.Path;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private NettyClient nettyClient;
    private static FileList list;
    private static State currentState = State.WAIT;
    private static String url;
    private static boolean listRefreshed;
    private static final Logger log = Logger.getLogger(NettyClientHandler.class);

    public NettyClientHandler(int port, String host) {
       nettyClient = new NettyClient(port, host);
    }

    public void connectServer() {
        nettyClient.connect();
    }

    public void disconnectServer() {
        nettyClient.disconnect();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException {
        if (msg == null) return;
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            byte read = buf.readByte();
            if (currentState == State.DOWNLOAD_FILE && read == CommandType.SEND_FILE.getCode()) {
                FileController.receiveFile(buf, url);
            }
            if (currentState == State.GET_LIST && read == CommandType.SEND_FILE_LIST.getCode()) {
                ByteBuf accum = ctx.alloc().directBuffer(1024 * 1024, 10 * 1024 * 1024);
                list = FileController.receiveFileList(buf, accum);
                if(list != null) {
                    listRefreshed = true;
                    log.info("files list is received");
                }
            }
        }
        buf.release();
        ctx.close();
        currentState = State.WAIT;
    }

/*    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(currentState == State.SEND_FILE) {
            FileController.sendFile(clientPath, ctx.channel(), future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
                if (future.isSuccess()) {
                    System.out.println("File sent successful");;
                }
            });
        }
        else if(currentState == State.DOWNLOAD_FILE) {
            FileController.downloadFile(ctx.channel(), fileName);
        }
        else if(currentState == State.GET_LIST) {
            FileController.sendRefresh(ctx.channel());
        }
        else if(currentState == State.DELETE_FILE) {
            FileController.sendDelete(ctx.channel(),fileName);
        }
        else currentState = State.WAIT;
    }*/

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void getFileList() {
        FileController.sendRefresh(nettyClient.getChannel());
        listRefreshed = false;
    }

    public void sendToCloud(Path path) throws IOException {
        FileController.sendFile(path, nettyClient.getChannel());
    }

    public void downloadFile(String fileName){
        FileController.sendDelete(nettyClient.getChannel(), fileName);
    }

    public void deleteFromCloud(String fileName){
        FileController.sendDelete(nettyClient.getChannel(),fileName);
    }

    public static FileList getList() {
        return list;
    }

    public void setCurrentState(State currentState) {
        NettyClientHandler.currentState = currentState;
    }
    public void setUrl(String url) {
        NettyClientHandler.url = url;
    }

    public static boolean isListRefreshed() {
        return listRefreshed;
    }
}
