import java.util.*;
import java.io.*;
import java.net.*;


public class Peer {
    /* Peer begins */
    public static String fileOwnerHostname = "localhost";
    public static int fileOwnerPort;
    public static int myPortAsServer;
    public static String neighborServerHostname = "localhost";
    public static int neighborServerPort;

    public static Socket socketWithFileOwner;
    public static ServerSocket listeningSocketAsServer;
    public static Socket socketWithNeighborClient;
    public static Socket socketWithNeighborServer;

    static String myID;

    static String fileName;
    static String chunkCount;

    Utility utility = new Utility();

    public static boolean isArgumentsValid(String[] arguments){
        return true;
    }
    public static void initialize(String[] arguments){
        fileOwnerPort = Integer.parseInt(arguments[0]);
        myPortAsServer = Integer.parseInt(arguments[2]);
        neighborServerPort = Integer.parseInt(arguments[3]);

        myID = fileOwnerHostname+fileOwnerPort+myPortAsServer+neighborServerHostname+neighborServerPort;
    }

    void getChunksFromFileOwner(){
        while(true){
            try{
                socketWithFileOwner = new Socket(fileOwnerHostname, fileOwnerPort);
                
                utility.sendString(socketWithFileOwner.getOutputStream(), "");
                
            } catch (Exception exception){
                System.err.println(exception.getLocalizedMessage());
            }
        }
    }
    public static void main(String[] arguments){
        Peer peer = new Peer();
        if(isArgumentsValid(arguments)){
            initialize(arguments);
            peer.getChunksFromFileOwner();
        } else {
            System.exit(0);
        }
    }
    /* Peer ends */

    /* ServerHandler begins */
    /* ServerHandler ends */

    /* ClientHandler begins */
    /* ClientHandler ends */

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

}
    /* Utility ends */
}