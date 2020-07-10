package ru.geekbrains.server;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.net.URL;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    private NettyServerHandler nettyServerHandler;
    private boolean isAlive;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nettyServerHandler = new NettyServerHandler();
    }

    public void start(ActionEvent actionEvent) {
        if(!isAlive) {
            nettyServerHandler.serverStart();
            isAlive = true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "server is already started", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void stop(ActionEvent actionEvent) {
        if(isAlive) {
            isAlive = false;
            nettyServerHandler.serverStop();
        } else  {
            Alert alert = new Alert(Alert.AlertType.ERROR, "server is already stopped", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
