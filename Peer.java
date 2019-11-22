import java.util.*;
import java.io.*;
import java.net.*;


public class Peer {
    /* Peer begins */
    public static String hostname = "localhost";

    public static int fileOwnerPort;
    public static int myPortAsServer;
    public static int neighborServerPort;
    
    public static ServerSocket listeningSocketAsServer;
    public static Socket socketWithNeighborClient;

    static String myID;

    static String fileName;
    static int totalChunkCount;
    static List<Integer> chunksIHave = new ArrayList<Integer>();

    Utility utility = new Utility();

    static String getFileNameByChunkID(int chunkID){
        return "part_"+chunkID;
    }
    public static boolean isNextServerFileowner(){
        return new Random().nextBoolean();
    }

    public static boolean isArgumentsValid(String[] arguments){
        return true;
    }

    public static boolean doINeedMoreChunks(){
        if (getCountOfChunksIHave() < getTotalCountOfChunks()){
            return true;
        }
        return false;
    }

    static int getTotalCountOfChunks() {
		return totalChunkCount;
    }
    static boolean doIHaveNoChunkAtAll(){
        return getCountOfChunksIHave() > 0;
    }
    static List<Integer> getMyChunks(){
        return chunksIHave;
    }
	static int getCountOfChunksIHave() {
		return getMyChunks().size();
    }
    static boolean canIHaveTheToken(){
        return isNextServerFileowner();
    }

	public static void main(String[] arguments){
        if(isArgumentsValid(arguments)){
            myID = fileOwnerPort+""+myPortAsServer+""+neighborServerPort;
            new ConsumeFileOwner(Integer.parseInt(arguments[0])).start();
            new ServePeer(Integer.parseInt(arguments[1])).start();
            new ConsumePeer(neighborServerPort = Integer.parseInt(arguments[2])).start();
        } else {
            System.exit(0);
        }
    }
    /* Peer ends */

     /* ConsumeFileOwner begins */
    private static class ConsumeFileOwner extends Thread {
        Socket socketWithFileOwner;
        int port;
        Utility utility = new Utility();
        ConsumeFileOwner(int fileOwnerPort){
            port = fileOwnerPort;
        }
        public void run(){
            while(doINeedMoreChunks()){
                if(canIHaveTheToken()){
                    try{
                        System.out.println(myID + ": Trying to connect to file owner at port "+port + "...");
                        socketWithFileOwner = new Socket(hostname, port);
                        
                        if(doIHaveNoChunkAtAll()){
                            utility.sendString(new ObjectOutputStream(socketWithFileOwner.getOutputStream()), myID+" init");
                            String[] response = utility.receiveString(new ObjectInputStream(socketWithFileOwner.getInputStream())).split(" ");
                            if(Boolean.parseBoolean(response[0])){
                                fileName = response[1];
                                totalChunkCount = Integer.parseInt(response[2]);
                            }
                        } else {
                            utility.sendString(new ObjectOutputStream(socketWithFileOwner.getOutputStream()), myID+" getlist");
                            String[] response = utility.receiveString(new ObjectInputStream(socketWithFileOwner.getInputStream())).split(" ");
                            if(Boolean.parseBoolean(response[0])){
                                List<Integer> myChunks = getMyChunks();
                                int desiredChunkID=-1;
                                for(int i=1;i<response.length;i++){
                                    if(!myChunks.contains(Integer.parseInt(response[i]))){
                                        desiredChunkID = Integer.parseInt(response[i]);
                                        break;
                                    }
                                }
                                if (desiredChunkID>0){
                                    utility.sendString(new ObjectOutputStream(socketWithFileOwner.getOutputStream()), myID+" get "+desiredChunkID);
                                    utility.receiveFile(getFileNameByChunkID(desiredChunkID), socketWithFileOwner.getInputStream());
                                } else {
                                    utility.sendString(new ObjectOutputStream(socketWithFileOwner.getOutputStream()), myID+" thanks");
                                }
                            }
                        }
                    } catch (Exception exception){
                        System.err.println(myID + ": Failed to connect to file owner at port "+port+".");
                    } finally {
                        if (socketWithFileOwner != null){
                            socketWithFileOwner.close();
                        }
                    }
                }
            }
        }
    }
    /* ConsumeFileOwner ends */

    /* ServePeer begins */
    private static class ServePeer extends Thread {
        int port;
        ServePeer(int myPortAsServer){
            port = myPortAsServer;
         }
        public void run(){

        }
    }
    /* ServePeer ends */

    /* ConsumePeer begins */
    private static class ConsumePeer extends Thread {
        Socket socketWithNeighborServer;
        int port;
        Utility utility = new Utility();
        ConsumePeer(int neighborServerPort){
            port = neighborServerPort;
         }
        public void run(){
            while(doINeedMoreChunks()){
                if(canIHaveTheToken()){
                    try{
                        System.out.println(myID + ": Trying to connect to file owner at port "+port + "...");
                        socketWithNeighborServer = new Socket(hostname, port);
                        
                        if(doIHaveNoChunkAtAll()){
                            utility.sendString(new ObjectOutputStream(socketWithNeighborServer.getOutputStream()), myID+" init");
                            String[] response = utility.receiveString(new ObjectInputStream(socketWithNeighborServer.getInputStream())).split(" ");
                            if(Boolean.parseBoolean(response[0])){
                                fileName = response[1];
                                totalChunkCount = Integer.parseInt(response[2]);
                            }
                        } else {
                            utility.sendString(new ObjectOutputStream(socketWithNeighborServer.getOutputStream()), myID+" getlist");
                            String[] response = utility.receiveString(new ObjectInputStream(socketWithNeighborServer.getInputStream())).split(" ");
                            if(Boolean.parseBoolean(response[0])){
                                List<Integer> myChunks = getMyChunks();
                                int desiredChunkID=-1;
                                for(int i=1;i<response.length;i++){
                                    if(!myChunks.contains(Integer.parseInt(response[i]))){
                                        desiredChunkID = Integer.parseInt(response[i]);
                                        break;
                                    }
                                }
                                if (desiredChunkID>0){
                                    utility.sendString(new ObjectOutputStream(socketWithNeighborServer.getOutputStream()), myID+" get "+desiredChunkID);
                                    utility.receiveFile(getFileNameByChunkID(desiredChunkID), socketWithNeighborServer.getInputStream());
                                } else {
                                    utility.sendString(new ObjectOutputStream(socketWithNeighborServer.getOutputStream()), myID+" thanks");
                                }
                            }
                        }
                    } catch (Exception exception){
                        System.err.println(myID + ": Failed to connect to file owner at port "+port+".");
                    } finally {
                        if (socketWithNeighborServer != null){
                            socketWithNeighborServer.close();
                        }
                    }
                }
            }
        }
    }
    /* ConsumePeer ends */

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
}

}