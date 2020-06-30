import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;


public class ClientController extends ChannelInboundHandlerAdapter {
    @FXML
    VBox clientPanel, serverPanel;

    private NettyClient nettyClient;
    private boolean isAlive;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void conSettings(ActionEvent actionEvent) {

    }

    public void connect(ActionEvent actionEvent) {
        if(!isAlive) {
            nettyClient = new NettyClient(8090, "localhost");
            new Thread(nettyClient).start();
            isAlive = true;
        } else throw new RuntimeException("client is already started");
    }


    public void disconnect(ActionEvent actionEvent) {
        if(isAlive) {
            nettyClient.disconnect();
            isAlive = false;
        } else throw new RuntimeException("client is disconnected");
    }
}
