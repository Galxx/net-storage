package common;

import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import messges.AbstractMsg;
import messges.FileTransferMsg;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FSWorker {

    /*
    Класс предназначен для работы с файловым хранилищем,
    описывает общие для клиентов и сервера методы.
    В конструкторе задаем корневой каталог.
    * */

    /*Поле предназначено для хранения корневого каталога.
     * На клиенте - это будет его папка на HDD, для сервера - каталог,
     * соответствующий nickname подключившегося клиента */
    private String rootDir;
    private File to;
    private OutputStream osFile;
    private static int bufferSize = 50092;//2092;



    /*Конструкторы*/
    public FSWorker(String rootDir) {
        this.rootDir = rootDir;
    }

    public FSWorker() {

    }

    /*
     * Setter для установки значения поля - корневого каталога пользователя
     * */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    /*
    Методы для работы с файлами и каталогами
    * */

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
//                logger.error("Ошибка создания директории!");
//                logger.error(e.getMessage());
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
    Удаляем файл или каталог с файлами
    * */
    public void delFsObject(String fsObjectName) {

        Path newPath = Paths.get(fsObjectName);
        Path pathToDelete = Paths.get(rootDir, "\\",
                newPath.subpath(2, newPath.getNameCount()).toString());

        if (Files.isDirectory(pathToDelete)) {
            deleteDirectory(pathToDelete);

        } else if (Files.isRegularFile(pathToDelete)) {
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

    //удаление каталога с файлами
    private void deleteDirectory(Path pathToDelete) {
//        try (Stream<Path> str = Files.list(pathToDelete)) {
//            str.sorted(Comparator.reverseOrder())
//                    .map(Path::toFile)
//                    .forEach(File::delete);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //удалаяем сам пустой каталог
//            //Files.delete(pathToDelete);
//        }

        try {
            Files.walkFileTree(pathToDelete, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println("delete file: " + file.toString());
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    System.out.println("delete dir: " + dir.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }




//        }
//
//        new Thread(() -> {
//            try () {
//                FileUtils.deleteDirectory(pathToDelete.toFile());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();

//        fullFolderPath = rootDir + nickname;

//        try {
//            Files.walk(pathToDirToDelete, FileVisitOption.FOLLOW_LINKS)
//                    .sorted(Comparator.reverseOrder())
//                    .forEach(path -> {
//                        try {
//                            Files.delete(path);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    });
//            //Files.delete(pathToDirToDelete);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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

}