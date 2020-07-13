package ru.geekbrains.client;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.apache.log4j.Logger;
import ru.geekbrains.common.FileList;
import ru.geekbrains.common.FileMessage;
import ru.geekbrains.common.State;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class ClientController implements Initializable{

    @FXML
    TextField ipAddress, port;

    @FXML
    TableView<FileMessage> clientFilesTable, serverFilesTable;

    @FXML
    TextField clientPathField, serverPathField;

    private NettyClientHandler nettyClientHandler;
    private static final String url = "./client/src/main/resources/data";
    private boolean isAlive;
    private static final Logger log = Logger.getLogger(NettyClientHandler.class);


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getContent(clientFilesTable, clientPathField);
        getContent(serverFilesTable, serverPathField);
        updateList(clientFilesTable, Paths.get(url),clientPathField);
        nettyClientHandler = new NettyClientHandler(Integer.parseInt(port.getText()), ipAddress.getText());
    }

    public void btnExitAction(ActionEvent actionEvent) {
        nettyClientHandler.disconnectServer();
        Platform.exit();
    }

    public void connect(ActionEvent actionEvent) throws InterruptedException {
        if (!isAlive) {
            nettyClientHandler.connectServer();
            nettyClientHandler.getNettyClient().getChannel().writeAndFlush(State.LIST_REQUEST);
            isAlive = true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "client is already connected", ButtonType.OK);
            alert.showAndWait();
        }
    }


    public void disconnect(ActionEvent actionEvent) {
        if(isAlive) {
            isAlive = false;
            nettyClientHandler.disconnectServer();
            serverFilesTable.getItems().clear();
        } else  {
            Alert alert = new Alert(Alert.AlertType.ERROR, "client is already disconnected", ButtonType.OK);
            alert.showAndWait();
        }

    }

    public void getContent(TableView<FileMessage> filesTable, TextField pathField) {
        TableColumn<FileMessage, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileMessage, String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setPrefWidth(200);

        TableColumn<FileMessage, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileMessage, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(100);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileMessage, String> fileDateColumn = new TableColumn<>("Last edit");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn,fileNameColumn,fileSizeColumn,fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        filesTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                if (Files.isDirectory(path)) {
                    updateList(filesTable,path,pathField);
                }
            }
        });
    }

    public void updateList(TableView<FileMessage> filesTable, Path path, TextField pathField) {

        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileMessage::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "can't update list of files", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnPathUpAction(TextField pathField, TableView<FileMessage> filesTable) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateList(filesTable,upperPath,pathField);
        }
    }

    public String getSelectedFilename(TableView<FileMessage> filesTable) {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getCurrentPath(TextField pathField) {
        return pathField.getText();
    }

    public void clientBtnPathUpAction(ActionEvent actionEvent) {
        btnPathUpAction(clientPathField, clientFilesTable);
    }

    public void serverBtnPathUpAction(ActionEvent actionEvent) {
        btnPathUpAction(serverPathField, serverFilesTable);
    }

    public void sendToCloudBtnAction(ActionEvent actionEvent) throws IOException, InterruptedException {
        File file = new File(getCurrentPath(clientPathField), getSelectedFilename(clientFilesTable));
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(file);
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(State.FILE_RECEIVE);
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(State.LIST_REQUEST);
    }

    public void deleteFromClientBtnAction(ActionEvent actionEvent) throws IOException {
        Path path = Paths.get(getCurrentPath(clientPathField), getSelectedFilename(clientFilesTable));
        Files.delete(path);
        updateList(clientFilesTable, path.getParent(), clientPathField);
    }

    public void downloadBtnAction(ActionEvent actionEvent) throws IOException {
        String fileName = serverFilesTable.getSelectionModel().getSelectedItem().getFilename();
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(fileName);
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(State.FILE_DOWNLOAD);
    }

    public void deleteFromCloudBtnAction(ActionEvent actionEvent) {
        String fn = serverFilesTable.getSelectionModel().getSelectedItem().getFilename();
        String fileName = serverFilesTable.getSelectionModel().getSelectedItem().getFilename();
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(fileName);
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(State.FILE_DELETE);
        nettyClientHandler.getNettyClient().getChannel().writeAndFlush(State.LIST_REQUEST);
    }

    public void updateServerTable(ActionEvent actionEvent) {
        FileList fl = NettyClientHandler.getList();
        updateList(serverFilesTable, Paths.get(fl.getUrl()), serverPathField);
        updateList(clientFilesTable, Paths.get(url), clientPathField);
    }

}
