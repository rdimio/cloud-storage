import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;


public class ClientController {
    @FXML
    VBox clientPanel, serverPanel;

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void conSettings(ActionEvent actionEvent) {

    }
}
