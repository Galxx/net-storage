package netstorage.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;


import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private Network network;

    @FXML
    TextField msgField;

    @FXML
    public ListView<String> listView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        network = new Network("localhost", 8189);
        network.connect();


//        network = new Network((args) -> {
//            Platform.runLater(() -> listView.getItems().add((String)args[0]));
//        });
    }

    public void sendMsgAction(ActionEvent actionEvent) {
//        network.sendMessage(msgField.getText());
//        msgField.clear();
//        msgField.requestFocus();
    }

    public void exitAction() {
        network.close();
        Platform.exit();
    }

    public void onMousePresset(MouseEvent mouseEvent) {

//        String selectedItem = listView.getSelectionModel().getSelectedItem();
//        String[] arr = selectedItem.split("[|]");
//        msgField.setText("/upload | "+ arr[0]);
    }

}