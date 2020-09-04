package netstorage.client;

import common.FSWorker;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import messges.*;
import messges.FileTransferMsg;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


public class Network {

    private String HOST;
    private int PORT;
    private static int bufferSize;
    private FSWorker fsWorker;

    public boolean isConnected;

    private Socket clientSocket;
    private ObjectDecoderInputStream odis;
    private ObjectEncoderOutputStream oeos;

    public Network(String HOST, int PORT) {
            this.HOST = HOST;
            this.PORT = PORT;
            fsWorker = new FSWorker();
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

        isConnected = false;
//        channel.close();
    }

    public void sendFile(String str) {

        Path path = Paths.get( "client\\clientstorage\\"+str);
        fsWorker.sendFileInParts(oeos,path);

    }

    public void downloadFile(String fileName) {
        AbstractMsg outObject = new CommandMsg(CommandMsg.DOWNLOAD_FILE, fileName);
        try {
            oeos.writeObject(outObject);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void startReadingThread(Controller controllerFX) {
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

//                            CommandMsg cmdMsg = (CommandMsg) incomingMsg;
//
//                            if (cmdMsg.getCommand() == CommandMsg.AUTH_OK) {
//                                System.out.println("AUTHOK");
//                                controllerFX.loginOk();
//                            } else if (cmdMsg.getCommand() == CommandMsg.CREATE_DIR) {
//                                System.out.println("CREATE_DIR");
//                                createDirectory(cmdMsg);
//                            }
                        }

                        //получаем из входящего сообщения список файлой
//                        if (incomingMsg instanceof FileListMsg) {
//                            filesList = ((FileListMsg) incomingMsg).getFileList();
//                            controllerFX.setCloudFilesList(filesList);
//                        }

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

        String rootDir = "client\\clientstorage\\";

        Path newFilePath = Paths.get(rootDir +
                msg.getFileName());

        fsWorker.mkFile(newFilePath, msg.getData(),msg.getislast(),msg.getisFirst());
    }

}