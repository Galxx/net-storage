package netstorage.client;

import common.FSWorker;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import messges.AbstractMsg;
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

    public void sendMessage(String str) {

        Path path = Paths.get( "client\\clientstorage\\"+str);
        fsWorker.sendFileInParts(oeos,path);

    }
}