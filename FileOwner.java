import java.net.*;
import java.io.*;
import java.util.*;

public class FileOwner {

    static ServerSocket serverSocket;
    /* FileOwner Class implementation is below */
    static int peerCount = 5;
    static int controlPort;
    static ServerSocket controlServerSocket;
    static int dataPort;
    static ServerSocket dataServerSocket;

    static String chunkFilePath = "./chunk.txt";

    public FileOwner(){
        File file = new File("./main.jpg");
        createChunksFromFile(file);
    }

    public int createChunksFromFile(File file){
        int chunkCount = 0;
        makeBlankFile(new File(chunkFilePath));
        /*saurabh*/
        return chunkCount;
    }

    public void createFileFromChunks(){
        /*saurabh*/
    }

    /* Utility begins */
    private class Utility {
        public void sendString(ObjectOutputStream objectOutputStream, String message) {
            try {
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        public String receiveString(ObjectInputStream objectInputStream){
            try{
                return (String) objectInputStream.readObject();
            } catch (Exception exception){
                
            }
            return null;
        }
        public void sendFile(String fileName, OutputStream outputStream) {
            try {
                File file = new File("./" + fileName);
                byte[] byteArray = new byte [(int)file.length()];
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                bufferedInputStream.read(byteArray,0,byteArray.length);
                outputStream.write(byteArray, 0, byteArray.length);
                outputStream.flush();
                outputStream.close();
            } catch (Exception exception) {
                System.out.println("\t" + exception.getLocalizedMessage() + "\n");
            } finally {
                try {
                    if(outputStream != null) {outputStream.close();}
                } catch (Exception exception) {
                    System.out.println("\t" + exception.getLocalizedMessage() + "\n");
                }
            }
        }
    
        public void receiveFile(String fileName, InputStream inputStream) {
            try {
                File someFile = new File("./" + fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(someFile);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                int bytesRead = 0;
                int b;
                while ((b = inputStream.read()) != -1) {
                    bufferedOutputStream.write(b);
                    bytesRead++;
                }
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                fileOutputStream.close();
                inputStream.close();
            } catch(Exception exception) {
                System.out.println("\t" + exception.getLocalizedMessage() + "\n");
            } finally {
                try {
                    if(inputStream != null) {inputStream.close();}
                } catch (Exception exception) {
                    System.out.println("\t" + exception.getLocalizedMessage() + "\n");
                }
            }
        }
    }
    /* Utility ends */

    public void makeBlankFile(File file){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("");
            writer.close();
        } catch(Exception exception) {
            System.err.println(exception.getLocalizedMessage());
        }
    }

    public String getChunkFilePathByChunkID(int chunkID){
        Scanner scanner = null;
        try {
            File file = new File(chunkFilePath);
            scanner = new Scanner(file);
            while(scanner.hasNextLine()){
                String[] row = scanner.nextLine().split(" ");
                int id = Integer.parseInt(row[0]);
                if(id == chunkID){
                    scanner.close();
                    return row[1];
                }
            }
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
        } finally {
            return null;
        }
    }

    public List<Integer> getChunkList(){
        List<Integer> list = new ArrayList<Integer>();

        try {
            File file = new File(chunkFilePath);
            Scanner scanner = new Scanner(file);
            while(scanner.hasNextLine()){
                String[] row = scanner.nextLine().split(" ");
                int chunkID = Integer.parseInt(row[0]);
                //int chunkFileName = Integer.parseInt(row[1]);
                list.add(chunkID);
            }
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
        }
        return list;
    }

    public static boolean isArgumentsValid(String[] arguments){
        return true;
    }

    public static void main(String[] arguments){
        if(isArgumentsValid(arguments)){
            try {
                serverSocket = new ServerSocket(Integer.parseInt(arguments[0]));
                try{
                    while(true){
                        new RequestHandler(serverSocket.accept()).start();
                    }
                } catch (Exception exception){
                    System.err.println(exception.getLocalizedMessage());
                }
            } catch(Exception exception){
                System.err.println(exception.getLocalizedMessage());
            } finally {
                if (serverSocket != null) {
                    try{
                        serverSocket.close();
                    } catch (Exception exception){
                        System.err.println(exception.getLocalizedMessage());
                    }
                }
            }
        } else {
            System.exit(0);
        }
    }

    private static class RequestHandler extends Thread {
        private Socket socket;
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;
        RequestHandler(Socket socket){
            this.socket = socket;
        }
        public void handleRequest(){

        }
        public void run(){
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.flush();
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                //handling request begins

                //handling request ends
            } catch (Exception exception){
                System.err.println(exception.getLocalizedMessage());
            } finally {
                try{
                    objectInputStream.close();
                    objectOutputStream.close();
                    socket.close();
                } catch (Exception exception){}
            }
        }
    }
}

