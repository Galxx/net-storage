package netstorage.client;

import common.FSWorker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import messages.CommandMsg;


import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private Network network;
    private String rootDir;

    @FXML
    public TextField usernameField;

    @FXML
    public PasswordField passwordField;

    @FXML
    public ListView localList;

    @FXML
    public ListView cloudList;

    @FXML
    public HBox authPanel;

    @FXML
    public HBox actionPanel1;

    @FXML
    public HBox actionPanel2;

    private ObservableList<String> localFilesList;
    private ObservableList<String> cloudFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        rootDir  = "client\\clientstorage\\";
        network = new Network("localhost", 8189,
                rootDir,
                (args) -> {
            int oper = (int)args[0];
            if(oper == CommandMsg.DOWNLOAD_FILE_OK){
                updateLocalFilesList();
            }else if(oper == CommandMsg.AUTH_OK){
                loginOk();
            }else if(oper == CommandMsg.REFRESH_CLOUD){
                refreshFolderList();
            }else if(oper == CommandMsg.LIST_FILES){
                List<String> fileList = (List<String>)args[1];
                setCloudFilesList(fileList);
            }

        });

        localFilesList = FXCollections.observableArrayList();
        localList.setItems(localFilesList);

        cloudFilesList = FXCollections.observableArrayList();
        cloudList.setItems(cloudFilesList);

        //устанавливаем реакцию на двойной клик
        localList.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    goDeeper((ListView) mouseEvent.getSource());
                }
            }
        });


        usernameField.setText("log1");
        passwordField.setText("pass1");
    }

    public void login() {
        if(network.connect()){
            String login = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            network.startReadingThread();
            network.doAuth(login, pass);
        };
    }


    //обновляем список файлов, находящийся в локальном хранилище
    public void updateLocalFilesList() {

        Platform.runLater(() ->{
            localList.getItems().clear();

            List<String> list = getLocalFilesList();

            if (list.size() > 0) {
                localFilesList.addAll(list);
            } else
                localFilesList.add("В локальной папке нет файлов!");

            localFilesList.add(0, "..");
        } );

    }

    public List<String> getLocalFilesList() {
        return FSWorker.getInstance().listDir(Paths.get(rootDir));
    }

    public void loginOk() {
        authPanel.setVisible(false);
        authPanel.setManaged(false);

        actionPanel1.setVisible(true);
        actionPanel2.setVisible(true);

        actionPanel1.setManaged(true);
        actionPanel2.setManaged(true);

        updateLocalFilesList();
        network.listCloudFiles(null);
    }

    public void setCloudFilesList(List<String> list) {

        if (list != null) {
            Platform.runLater(() -> {
                cloudFilesList.clear();
                if (list.size() > 0) {
                    cloudFilesList.addAll(list);
                    cloudFilesList.add(0, "..");
                } else
                    cloudFilesList.add("На сервере нет файлов");
            });
        }

    }

    public void refreshFolderList() {
        network.listCloudFiles(null);
    }

    public void uploadFileOrFolder(ActionEvent event) {
        String itemName = localList.getItems()
                .get(localList
                        .getFocusModel()
                        .getFocusedIndex())
                .toString();

        network.sendFile(itemName);
        refreshFolderList();
    }

    //метод отправляет на сервер запрос на закачку файла в локальное хранилище
    public void downloadFile(ActionEvent event) {
        String fileName = cloudList.getItems().get(
                cloudList.getFocusModel().getFocusedIndex()).toString();
        System.out.println(fileName);

        network.downloadFile(fileName);
    }

    //метод для отправки команды на удаление файла в облачном хранилище
    public void deleteCloudFsObj(ActionEvent event) {
        String fileName = cloudList.getItems().get(
                cloudList.getFocusModel().getFocusedIndex()).toString();
        System.out.println("Delete from cloud storage" + fileName);

        network.deleteCloudFsObj(fileName);
    }

    //при двойном нажатии на папке, переходим в нее
    //при двойном нажатии на "..", переходим на уровень выше
    private void goDeeper(ListView listView) {
        String itemName = listView.getItems()
                .get(listView
                        .getFocusModel()
                        .getFocusedIndex())
                .toString();

        if (itemName.equals("..")) {
            //переход на уровень выше
                rootDir = Paths.get(rootDir).getParent().toString()+"\\";
                updateLocalFilesList();
        } else {
            //переход в каталог с именем itemName
            Path path = Paths.get(rootDir, itemName);
            if (Files.isDirectory(path)) {
                rootDir = rootDir+ "\\" + itemName + "\\";
                updateLocalFilesList();
            }
        }

        network.setRootDir(rootDir);


    }

    public void exitAction() {
        network.close();
        Platform.exit();
    }

}