package common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import messages.AbstractMsg;
import messages.FileTransferMsg;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public  class FSWorker {

    /*
    Класс предназначен для работы с файловым хранилищем,
    описывает общие для клиентов и сервера методы.
    В конструкторе задаем корневой каталог.
    * */
    private static FSWorker ourInstance = new FSWorker();

    private File to;
    private OutputStream osFile;
    private static int bufferSize = 50092;//2092;

    private FSWorker() {

    }

    public static FSWorker getInstance() {
        return ourInstance;
    }

    /*
     * Просмотр содержимого каталога.
     * На вход метода подается путь к каталогу.
     * */
    public List<String> listDir(Path dirPath) {
        List<String> fileList = new ArrayList<>();

        try (DirectoryStream<Path> str = Files.newDirectoryStream(dirPath)) {

            str.forEach(path -> fileList.add(
                    path.getFileName().toString()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileList;
    }

    /*
     * Создание каталога
     * */
    public void mkDir(Path newPath) {
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка создания директории!");

        }
    }

    /*
     * Создание файла
     * через IO, т.к большиние файлы по NIO не проходят
     * */
    public void mkFile(Path newFilePath, byte[] data, boolean isLast, boolean isFirst) {

        try {

            if (isFirst){
                to = new File(newFilePath.toString());
                if (Files.exists(newFilePath)){
                    to.delete();
                }
                osFile = new FileOutputStream(to);
                osFile.write(data);
            }else{
               osFile.write(data);
            }

            if(isLast){
                osFile.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    Удаляем файл
    * */
    public void delFsObject(String fsObjectName) {

        Path pathToDelete = Paths.get(fsObjectName);

        if (Files.isRegularFile(pathToDelete)) {
            deleteFileFromStorage(pathToDelete);
        }
    }

    //удаление файла из хранилища по пути к файлу
    public void deleteFileFromStorage(Path pathToDelete) {
        try {
            Files.delete(pathToDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void sendFileInParts(ObjectEncoderOutputStream oeos, Path path){

        try {
            InputStream isFile = new FileInputStream(path.toString());

            long fileSize = Files.size(path);
            long lastPackage;
            if(fileSize%bufferSize > 0){
                lastPackage = fileSize/bufferSize +1;
            }else{
                lastPackage = fileSize/bufferSize;
            }


            byte[] buffer = new byte[bufferSize];
            int count = 0;
            int i= 0;
            while ((count = isFile.read(buffer)) != -1) {
                i++;
                boolean isLast;

                if (lastPackage == i){
                    isLast = true;
                }else{
                    isLast = false;
                }

                AbstractMsg outObject;

                if (count < bufferSize){
                    outObject = new FileTransferMsg(path, Arrays.copyOfRange(buffer, 0, count),isLast, i==1);
                }else{
                    outObject = new FileTransferMsg(path,buffer,isLast,i == 1);
                }
                oeos.writeObject(outObject);
            }
            isFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendFileInParts(ChannelHandlerContext ctx, Path path){

        try {
            InputStream isFile = new FileInputStream(path.toString());

            long fileSize = Files.size(path);
            long lastPackage;
            if(fileSize%bufferSize > 0){
                lastPackage = fileSize/bufferSize +1;
            }else{
                lastPackage = fileSize/bufferSize;
            }


            byte[] buffer = new byte[bufferSize];
            int count = 0;
            int i= 0;
            while ((count = isFile.read(buffer)) != -1) {
                i++;
                boolean isLast;

                if (lastPackage == i){
                    isLast = true;
                }else{
                    isLast = false;
                }

                AbstractMsg outObject;

                if (count < bufferSize){
                    outObject = new FileTransferMsg(path, Arrays.copyOfRange(buffer, 0, count),isLast, i==1);
                }else{
                    outObject = new FileTransferMsg(path,buffer,isLast,i == 1);
                }
                ctx.writeAndFlush(outObject);
            }
            isFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}