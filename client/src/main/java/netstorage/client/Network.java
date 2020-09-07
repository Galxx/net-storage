package netstorage.client;

import common.FSWorker;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import messages.*;
import messages.FileTransferMsg;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class Network {

    private String HOST;
    private int PORT;
    private String rootDir;
    private Callback callback;

    public boolean isConnected;

    private List<String> filesList;

    private Socket clientSocket;
    private ObjectDecoderInputStream odis;
    private ObjectEncoderOutputStream oeos;

    public Network(String HOST, int PORT, String rootDir, Callback callback) {
            this.HOST = HOST;
            this.PORT = PORT;
            this.rootDir = rootDir;
            this.callback = callback;

    }

    public void setRootDir(String rootDir){
        this.rootDir = rootDir;
    }

    public boolean connect() {
        try {
            clientSocket = new Socket(HOST, PORT);
            oeos = new ObjectEncoderOutputStream(clientSocket.getOutputStream());
            odis = new ObjectDecoderInputStream(clientSocket.getInputStream());
            isConnected = true;
            System.out.println("Подключились к серверу");
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
        return isConnected;
    }

    public void close() {

        if (isConnected) {
            try {
                oeos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                odis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        isConnected = false;
//        channel.close();
    }

    public void sendFile(String str) {

        Path path = Paths.get(rootDir +"\\"+str);
        FSWorker.getInstance().sendFileInParts(oeos,path);

    }

    public void downloadFile(String fileName) {
        AbstractMsg outObject = new CommandMsg(CommandMsg.DOWNLOAD_FILE, fileName);
        sendObject(outObject);
    }

    public void startReadingThread() {
        Thread thread = new Thread(() -> {

            while (isConnected) {
                //ожидаем поступления сообщения и считываем его в объект
                Object msg = readObject();

                if (msg != null) {
                    System.out.println("One message received" + msg.toString());

                    if (msg instanceof AbstractMsg) {
                        AbstractMsg incomingMsg = (AbstractMsg) msg;
                        //если поступило сообщение с командой - обработаем его
                        if (incomingMsg instanceof CommandMsg) {

                            CommandMsg cmdMsg = (CommandMsg) incomingMsg;

                            if (cmdMsg.getCommand() == CommandMsg.AUTH_OK
                            ||cmdMsg.getCommand() == CommandMsg.DOWNLOAD_FILE_OK
                            ||cmdMsg.getCommand() == CommandMsg.REFRESH_CLOUD) {
                                callback.callback(cmdMsg.getCommand());
                            }
                        }

                        //получаем из входящего сообщения список файлой
                        if (incomingMsg instanceof FileListMsg) {
                            filesList = ((FileListMsg) incomingMsg).getFileList();
                            callback.callback(CommandMsg.LIST_FILES,filesList);
                        }

                        //принимаем и сохраняем в локальное хранилище файл
                        if (incomingMsg instanceof FileTransferMsg) {
                            saveFileToLocalStorage((FileTransferMsg) incomingMsg);
                        }
                    }
                }
            }

        });

        //назначаем потом - демоном, чтобы об завершил работу сразу после закрытия
        // основного приложения
        thread.setDaemon(true);
        thread.start();
    }

    public Object readObject() {
        Object incomingObj = null;
        try {
            incomingObj = odis.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return incomingObj;
    }

    private void saveFileToLocalStorage(FileTransferMsg msg) {

        Path newFilePath = Paths.get(rootDir +
                msg.getFileName());

        FSWorker.getInstance().mkFile(newFilePath, msg.getData(),msg.getislast(),msg.getisFirst());
    }

    public void doAuth(String login, String pwd) {

        AbstractMsg outObject = new AuthMsg(login, pwd);
        try {
            oeos.writeObject(outObject);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listCloudFiles(String itemName) {
        sendObject(new CommandMsg(CommandMsg.LIST_FILES));
    }
    private void sendObject(AbstractMsg outObject){
        try {
            oeos.writeObject(outObject);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //отправляем команду на удаление файла в облачном хранилище по имени
    public void deleteCloudFsObj(String fileName) {
      sendObject(new CommandMsg(CommandMsg.DELETE, fileName));
    }


}