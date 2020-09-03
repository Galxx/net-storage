package netstorage.server;


import common.*;
import messges.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final List<Channel> channels = new ArrayList<>();
    private static int newClientIndex = 1;
    private String clientName;
    private boolean isLogged;
    private FSWorker fsWorker;

    public MainHandler() {
        fsWorker = new FSWorker();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Клиент подключился: " + ctx);
        channels.add(ctx.channel());
        clientName = "Клиент #" + newClientIndex;
        newClientIndex++;
        isLogged = true;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (msg == null)
                return;

            if (msg instanceof AbstractMsg) {
                processMsg((AbstractMsg) msg, ctx);
            } else {
                System.out.println("Server received wrong object!");
                //logger.debug("Server received wrong object!");
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }

//        System.out.println("Получено сообщение: " + s);
//        if (s.startsWith("/")) {
//            if (s.startsWith("/changename ")) { // /changename myname1
//                String newNickname = s.split("\\s", 2)[1];
//                broadcastMessage("SERVER", "Клиент " + clientName + " сменил ник на " + newNickname);
//                clientName = newNickname;
//            }
//            return;
//        }
//        broadcastMessage(clientName, s);
    }

    //обрабатываем поступившее сообшение в зависимости от класса
    private void processMsg(AbstractMsg msg, ChannelHandlerContext ctx) {

        System.out.println("username: " + clientName);

        //обрабатываем остальные сообщения только, если прошли аутентификацию в БД
        if (isLogged) {
            if (msg instanceof FileTransferMsg) {
                saveFileToStorage((FileTransferMsg) msg);
//            } else if (msg instanceof CommandMsg) {
//                System.out.println("Server received a command " +
//                        ((CommandMsg) msg).getCommand());
//                logger.debug("Server received a command", msg);
//                processCommand((CommandMsg) msg, ctx);
            }
        } else {
            //вызываем проверку аутентификационных данных в БД
//            if (msg instanceof AuthMsg) {
//                System.out.println("Nickname in CloudServerHandler" +
//                        ((AuthMsg) msg).getNickname());
//                checkAuth((AuthMsg) msg, ctx);
//            }
        }
    }


    //получаем файл в виде объекта, записываем его в хранилище, в папку пользователя
    private void saveFileToStorage(FileTransferMsg msg) {

        String currentFolderPath = "server/filestorage";
        //получаем путь файла из локального хранилища
        Path filePath = Paths.get(msg.getPath());

        //отбрасываем корневой каталог из локального хранилища
        String relPath = filePath.subpath(1, filePath.getNameCount()).toString();

        //складываем пути
        Path newFilePath = Paths.get(currentFolderPath + "/" + relPath);

        //создаем файл в облачном хранилище
        fsWorker.mkFile(newFilePath, msg.getData());

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Клиент " + clientName + " вышел из сети");
        channels.remove(ctx.channel());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Клиент " + clientName + " отвалился");
        channels.remove(ctx.channel());
        ctx.close();
    }
}