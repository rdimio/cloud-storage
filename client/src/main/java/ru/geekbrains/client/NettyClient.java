package ru.geekbrains.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class NettyClient extends Thread {

    private final int port;
    private final String host;
    private EventLoopGroup workerGroup;
    private SocketChannel channel;

    public NettyClient(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void connect() {
        Thread t = new Thread(() -> {
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .remoteAddress(new InetSocketAddress(host,port))
                        .handler(new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                socketChannel.pipeline().addLast(new NettyClientHandler(port, host));
                                channel = socketChannel;
                            }

                        });
                ChannelFuture future = b.connect().sync();

                future.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
            }
        });

        t.setDaemon(true);
        t.start();
    }

    public void disconnect() {
        channel.close();
    }
}
