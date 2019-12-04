import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;

public class FileOwner {

    static ServerSocket serverSocket;
    /* FileOwner Class implementation is below */
    static String fileName = "test.pdf";
    static String chunkFilePath = "./temp/chunk.txt";
    static int chunkCount = 0;

    public static void initializeChunks(){
        System.out.println("Segmenting " + fileName + " into chunks...");
        createTempraryDirectoryAndChunkFile(chunkFilePath);
        File file = new File("./" + fileName);
        createChunksFromFile(file);
        createChunkDatabase();
        System.out.println("Segmented " + fileName + " into " + chunkCount + " chunks.");
    }

    public static void createChunkDatabase(){
        try{
            File chunkDatabase = new File(chunkFilePath);
            FileWriter fileWriter = new FileWriter(chunkDatabase);
            fileWriter.write(fileName + " " + chunkCount);
            for(int i=1;i<=chunkCount;i++){
                fileWriter.write("\n"+i+" "+"./temp/"+fileName+i);
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public static void createChunksFromFile(File file){
        RandomAccessFile randomAccessFile = null;
        int chunkSize = 102400;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {e.printStackTrace();}
        FileChannel channel = randomAccessFile.getChannel();
        int chunkID = 1;
        ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
        while(true) {
            try {
                if (!(channel.read(buffer) > 0)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            File chunkFile = new File("./temp/" + file.getName()+chunkID);
            FileChannel chunkFileChannel = null;
            try {
                chunkFileChannel = new FileOutputStream(chunkFile).getChannel();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            buffer.flip();
            try {
                chunkFileChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            buffer.clear();
            try {
                chunkFileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            chunkID++;
            chunkCount++;
            try {
                chunkFileChannel.close();
            } catch (Exception exception){
                //System.err.println("\t" + exception.getLocalizedMessage() + "\n");
            }
        }
    }

    

    public static void createTempraryDirectoryAndChunkFile(String filePath){
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("./temp"));
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write("");
            writer.close();
        } catch(Exception exception) {
            System.err.println(exception.getLocalizedMessage());
        }
    }

    public static String getChunkFilePathByChunkID(int chunkID){
        Scanner scanner = null;
        try {
            File file = new File(chunkFilePath);
            scanner = new Scanner(file);
            scanner.nextLine();
            while(scanner.hasNextLine()){
                String[] row = scanner.nextLine().split(" ");
                int id = Integer.parseInt(row[0]);
                if(id == chunkID){
                    return row[1];
                }
            }
        } catch (Exception exception) {
            System.err.println(exception.getLocalizedMessage());
        } finally {
            try {
                scanner.close();
            } catch (Exception exception){
                System.err.println("\t" + exception.getLocalizedMessage() + "\n");
            }
        }
        return null;
    }

    public static List<Integer> getChunkList(){
        List<Integer> list = new ArrayList<Integer>();

        try {
            File file = new File(chunkFilePath);
            Scanner scanner = new Scanner(file);
            scanner.nextLine();
            while(scanner.hasNextLine()){
                String[] row = scanner.nextLine().split(" ");
                int chunkID = Integer.parseInt(row[0]);
                list.add(chunkID);
            }
            try {
                scanner.close();
            } catch (Exception exception){}
        } catch (Exception exception) { exception.printStackTrace();}

        return list;
    }

    public static boolean isArgumentsValid(String[] arguments){
        boolean argumentPresent = arguments.length>0;
        boolean integerPort = false;
        if(argumentPresent){
            try {
                Integer.valueOf(arguments[0]);
                integerPort = true;
            } catch(Exception exception){
            }
            if(arguments.length > 1){
                fileName = arguments[1];
            }
        }
        return argumentPresent && integerPort;
    }

    public static void main(String[] arguments){
        if(isArgumentsValid(arguments)){
            try {
                initializeChunks();
                serverSocket = new ServerSocket(Integer.parseInt(arguments[0]));
                System.out.println("Listening at port " + arguments[0] + ".");
                try{
                    while(true){
                        new RequestHandler(serverSocket.accept());
                    }
                } catch (Exception exception){exception.printStackTrace();}
            } catch (Exception exception){
                System.err.println("Invalid Server Configuration");
                System.exit(0);
            } finally {
                try{
                    serverSocket.close();
                } catch (Exception exception){exception.printStackTrace();}
            }
        } else {
            System.err.println("Invalid Input");
            System.exit(0);
        }
    }

    private static class RequestHandler extends Thread {
        Socket socket;
        Utility utility = new Utility();
        RequestHandler(Socket socket){
            this.socket = socket;
            try {
                this.handleRequest();
            } catch(Exception exception){exception.printStackTrace();}
            finally {
                try {
                    socket.close();
                } catch (Exception exception){exception.printStackTrace();}
            }
        }
        public void handleRequest(){
            try{
                String request = utility.receiveString(new ObjectInputStream(socket.getInputStream()));
                String response = "";
                String[] requestSplitted = request.split(" ");
                String requesterID = requestSplitted[0];
                String requestType = requestSplitted[1];
                System.out.println("["+requesterID +"] "+requestType + ((requestSplitted.length>2) ? " "+requestSplitted[2] : ""));
                switch(requestType){
                    case "init":
                        response = "true " + fileName + " " + chunkCount;
                        System.out.println("RESPOND ["+requesterID+"]  with file name and total chunk count" + response);
                        utility.sendString(new ObjectOutputStream(socket.getOutputStream()), response);
                        break;
                    case "getlist":
                        List<Integer> chunkList = getChunkList();
                        response = "true";
                        for(int i=0;i<chunkList.size();i++){
                            response += " " + chunkList.get(i);
                        }
                        System.out.println("SENDING my chunk list to peer ["+requesterID+"] ");
                        utility.sendString(new ObjectOutputStream(socket.getOutputStream()), response);
                        break;
                    case "get":
                        String desiredFilePath = getChunkFilePathByChunkID(Integer.parseInt(requestSplitted[2]));
                        //System.out.println("RESPOND ["+requesterID+"]  with chunk "+ requestSplitted[2] + " from path " + desiredFilePath);
                        System.out.println("RESPOND to peer ["+requesterID+"]  by sending file with chunk id " + requestSplitted[2]);
                        utility.sendFile(desiredFilePath, socket.getOutputStream());
                        System.out.println("Successfully sent chunk " +requestSplitted[2] + " to ["+requesterID+"]");
                        break;
                    case "thanks":
                        break;
                }
            } catch (Exception exception){exception.printStackTrace();}
        }
    }
}


/* Utility begins */
final class Utility {
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
    public String convertFileIntoString(String filePath){
        try {
            File file = new File(filePath);
            byte[] byteArray = new byte [(int)file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(byteArray,0,byteArray.length);
        } catch (Exception exception) {
            System.out.println("\t" + exception.getLocalizedMessage() + "\n");
        }
        return "";
    }
    public void sendFile(String filePath, OutputStream outputStream) {
        //System.out.println("path " + filePath);
        try {
            File file = new File(filePath);
            //System.out.println("path " + filePath + " name " + file.getName());
            byte[] byteArray = new byte [(int)file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedInputStream.read(byteArray,0,byteArray.length);
            outputStream.write(byteArray, 0, byteArray.length);
            outputStream.flush();
            outputStream.close();
        } catch (Exception exception) { exception.printStackTrace();} 
        finally {
            try {
                if(outputStream != null) {outputStream.close();}
            } catch (Exception exception) { exception.printStackTrace();}
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
                System.err.println("\t" + exception.getLocalizedMessage() + "\n");
            }
        }
    }
}
/* Utility ends */