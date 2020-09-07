package netstorage.server;


import common.*;
import messages.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final List<Channel> channels = new ArrayList<>();
    private static int newClientIndex = 1;
    private String clientName;
    private boolean isLogged;
    private String currentFolderPath;

    public MainHandler() {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Клиент подключился: " + ctx);
        channels.add(ctx.channel());
        clientName = "Клиент #" + newClientIndex;
        newClientIndex++;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)  {

        try {
            if (msg == null)
                return;

            if (msg instanceof AbstractMsg) {
                processMsg((AbstractMsg) msg, ctx);
            } else {
                System.out.println("Server received wrong object!");
                //logger.debug("Server received wrong object!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            ReferenceCountUtil.release(msg);
        }

    }

    //обрабатываем поступившее сообшение в зависимости от класса
    private void processMsg(AbstractMsg msg, ChannelHandlerContext ctx) {

        System.out.println("username processMsg: " + clientName);

        //обрабатываем остальные сообщения только, если прошли аутентификацию в БД
        if (isLogged) {
            if (msg instanceof FileTransferMsg) {
                saveFileToStorage((FileTransferMsg) msg);
            } else if (msg instanceof CommandMsg) {
                System.out.println("Server received a command " +
                        ((CommandMsg) msg).getCommand());
//                logger.debug("Server received a command", msg);
                processCommand((CommandMsg) msg, ctx);
            }
        } else {
            //вызываем проверку аутентификационных данных в БД
            if (msg instanceof AuthMsg) {
                System.out.println("Nickname in CloudServerHandler" +
                        ((AuthMsg) msg).getNickname());
                checkAuth((AuthMsg) msg, ctx);
            }
        }
    }


    //получаем файл в виде объекта, записываем его в хранилище, в папку пользователя
    private void saveFileToStorage(FileTransferMsg msg) {

        //получаем путь файла из локального хранилища
        Path filePath = Paths.get(msg.getPath());

        Path newFilePath = Paths.get(currentFolderPath + "\\" + filePath.getFileName().toString());

        //создаем файл в облачном хранилище
        FSWorker.getInstance().mkFile(newFilePath, msg.getData(),msg.getislast(),msg.getisFirst());

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Клиент " + clientName + " вышел из сети");
        channels.remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.toString());
        System.out.println("Клиент " + clientName + " отвалился");
        channels.remove(ctx.channel());
        ctx.close();
    }

    //обрабатываем поступившую команду и отправляем ответ на нее клиенту
    private void processCommand(CommandMsg msg, ChannelHandlerContext ctx) {

        switch (msg.getCommand()) {
            case CommandMsg.LIST_FILES:
                sendFileList(msg, ctx);
                break;
            case CommandMsg.DOWNLOAD_FILE:
                sendFile(msg, ctx);
                break;
            case CommandMsg.DELETE:
                deleteFile(msg, ctx);
                break;
            case CommandMsg.CREATE_DIR:
                //createDirectory(msg);
                break;
        }
    }

    //отправить файл клиенту
    private void sendFile(CommandMsg msg, ChannelHandlerContext ctx) {

            Path filePath = Paths.get(currentFolderPath + "\\",
                    (String) (msg.getObject()[0]));

            FSWorker.getInstance().sendFileInParts(ctx,filePath);
            sendData(new CommandMsg(CommandMsg.DOWNLOAD_FILE_OK), ctx);

    }

    private void checkAuth(AuthMsg incomingMsg, ChannelHandlerContext ctx) {
        if (incomingMsg != null) {

            //получаем значение nickname из Auth Handler
            clientName = incomingMsg.getNickname();
            if (clientName != null) {

                //установить начальное значение папки просмотра
                String rootDir = "server\\filestorage\\";
                currentFolderPath = rootDir + clientName + "\\";

                //проверим что он существует
                Path path = Paths.get(currentFolderPath);
                if (!Files.exists(path)) {
                    FSWorker.getInstance().mkDir(path);
                }


                System.out.println("Client Auth OK");
                isLogged = true;

                sendData(new CommandMsg(CommandMsg.AUTH_OK), ctx);

                //logger.debug("Client Auth OK");
            } else {
                System.out.println("Client not found");
                isLogged = false;
                //logger.debug("Client not found");
            }
        }
    }

    //метод для отправки данных
    private void sendData(AbstractMsg msg, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(msg);
    }

    private void sendFileList(CommandMsg msg, ChannelHandlerContext ctx) {
        System.out.println("Отправляем список файлов");
        Object[] foldername = msg.getObject();
        if(foldername.length > 0){
            sendData(new FileListMsg(getClientFilesList(msg.getObject()[0])), ctx);
        }else sendData(new FileListMsg(getClientFilesList(null)), ctx);
    }

    private List<String> getClientFilesList(Object folderName) {

        List<String> fileList;
        Path listFolderPath = Paths.get(currentFolderPath);

        System.out.println("current currentFolderPath = " + currentFolderPath);
        fileList = FSWorker.getInstance().listDir(listFolderPath);

        return fileList;
    }

    private void deleteFile(CommandMsg msg, ChannelHandlerContext ctx) {

        String folderName = currentFolderPath +"\\"+msg.getObject()[0];
        FSWorker.getInstance().delFsObject(folderName);
        sendData(new CommandMsg(CommandMsg.REFRESH_CLOUD), ctx);
    }

}