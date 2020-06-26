import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.concurrent.Executors;


public class ClientController {
    @FXML
    VBox clientPanel, serverPanel;

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }


    public void conClient(ActionEvent actionEvent) throws IOException {
        Executors.newSingleThreadExecutor().execute(new NIOClient());
    }
}
