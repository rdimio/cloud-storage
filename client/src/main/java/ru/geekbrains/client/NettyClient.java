package ru.geekbrains.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;
import ru.geekbrains.server.NettyServer;

import java.net.InetSocketAddress;

public class NettyClient extends Thread {

    private final int port;
    private final String host;
    private SocketChannel channel;
    private static final Logger log = Logger.getLogger(NettyServer.class);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NettyClient(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void connect() {
        Thread t = new Thread(() -> {

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
                log.info("Client started");
                ChannelFuture future = b.connect().sync();

                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error(e.getStackTrace());
                e.printStackTrace();
            }
        });

        t.setDaemon(true);
        t.start();
    }

    public void disconnect() {
        workerGroup.shutdownGracefully();
        channel.close();
        log.info("Shutdown client");
    }
}
