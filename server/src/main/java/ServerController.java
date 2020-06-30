import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerController extends ChannelInboundHandlerAdapter {

    private NettyServer server;
    private boolean isAlive;


    public ServerController() {
        server = new NettyServer(8090);
    }

    public void serverStart(ActionEvent actionEvent) {
        if(!isAlive) {
            new Thread(server).start();
            isAlive = true;
        } else throw new RuntimeException("server is already started");
    }

    public void serverStop(ActionEvent actionEvent) throws IOException {
        if(isAlive) {
            server.disconnect();
            isAlive = false;
        } else throw new RuntimeException("server is stopped");
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
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected!");
//        Path path = Paths.get("C:\\Users\\Vadim\\Documents\\CloudStorage\\resources\\1.txt");
    }

}
