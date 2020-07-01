import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class ClientController extends ChannelInboundHandlerAdapter implements Initializable{

    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    TextField ipAddress;

    @FXML
    TextField port;

    @FXML
    TableView<FileMessage> clientFilesTable;

    @FXML
    TableView<FileMessage> serverFilesTable;

    @FXML
    TextField clientPathField;

    private NettyClient nettyClient;
    private boolean isAlive;
    private ServerController serverController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getContent(clientFilesTable, clientPathField);
        getContent(serverFilesTable, null);
        updateList(clientFilesTable, Paths.get("./client/src/main/resources/data"),clientPathField);
        serverController = new ServerController();
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        List<FileMessage> fl = (List<FileMessage>) msg;

            File[] fl = (File[]) msg;
        System.out.println(fl);
/*        serverFilesTable.getItems().clear();
        serverFilesTable.setItems(fl);
        serverFilesTable.sort();
        getContent(serverFilesTable, null);*/
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void connect(ActionEvent actionEvent) {
        if(!isAlive) {
            serverController.serverStart();
            nettyClient = new NettyClient( Integer.parseInt(port.getText()), ipAddress.getText());
            new Thread(nettyClient).start();
            isAlive = true;
        } else throw new RuntimeException("client is already started");
    }


    public void disconnect(ActionEvent actionEvent) {
        if(isAlive) {
            nettyClient.disconnect();
            isAlive = false;
            serverController.serverStop();
            System.out.println("Client disconnected!");
        } else throw new RuntimeException("client is disconnected");
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

    public void btnPathUpAction(TextField pathField) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateList(clientFilesTable,upperPath,pathField);
        }
    }

    public void clientBtnPathUpAction(ActionEvent actionEvent) {
        btnPathUpAction(clientPathField);
    }

    public void sendToCloudBtnAction(ActionEvent actionEvent) {
    }

    public void deleteFromClientBtnAction(ActionEvent actionEvent) {
    }

    public void downloadBtnAction(ActionEvent actionEvent) {
    }

    public void deleteFromCloudBtnAction(ActionEvent actionEvent) {
    }
}
