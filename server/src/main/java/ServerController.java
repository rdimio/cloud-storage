import javafx.event.ActionEvent;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerController {

    private NIOServer server;
    private ExecutorService executor;
    private boolean isAlive;


    public ServerController() throws IOException {
        server = new NIOServer();
        executor = Executors.newSingleThreadExecutor();
    }

    public void serverStart(ActionEvent actionEvent) {
        if(!isAlive) {
            executor.execute(server);
            isAlive = true;
        } else throw new RuntimeException("server is already started");
    }

    public void serverStop(ActionEvent actionEvent) throws IOException {
        if(isAlive) {
            server.stop();
            executor.shutdownNow();
            isAlive = false;
        } else throw new RuntimeException("server is stopped");
    }

}
