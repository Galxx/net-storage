package netstorage.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network {

    private static String HOST;
    private static int PORT;

    public boolean isConnected;

    private Socket clientSocket;
    private ObjectDecoderInputStream odis;
    private ObjectEncoderOutputStream oeos;

    public Network(String HOST, int PORT) {
            this.HOST = HOST;
            this.PORT = PORT;

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
//        channel.writeAndFlush(str);
    }
}