import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.*;
import java.net.*;

public class PeerHandler {
    static String fileName;
    static int totalChunkCount;
    static List<Integer> chunksIHave = new ArrayList<Integer>();
    Map<Integer, Integer> statistics = new HashMap<Integer, Integer>();
    static List<Integer> alreadyRequestedChunkList = new CopyOnWriteArrayList<Integer>();
    //
    boolean isInitiated = false;
    int myPort;

    //
    public static void createFileFromChunks(String fileName, int chunkCount) {
        final int BUFFERSIZE = 102400;
        File output = new File("./" + fileName);
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output), BUFFERSIZE);
            byte[] bytes = new byte[BUFFERSIZE];
            int bytesRead;
            for (int chunkID = 1; chunkID <= chunkCount; chunkID++) {
                String fullName = "./temp/" + fileName + chunkID;
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(fullName));
                while ((bytesRead = in.read(bytes, 0, bytes.length)) != -1) {
                    out.write(bytes, 0, bytesRead);
                }
                try {
                    in.close();
                } catch (Exception exception) {
                    System.out.println("\t" + exception.getLocalizedMessage() + "\n");
                }
            }
            out.close();
        } catch (Exception exception) {
            System.out.println("\t" + exception.getLocalizedMessage() + "\n");
        }
    }

    static String getFilePathByChunkID(int chunkID) {
        return "./temp/" + fileName + chunkID;
    }

    boolean doINeedMoreChunks() {
        for (int i = 1; i <= totalChunkCount; i++) {
            if (!chunksIHave.contains(i)) {
                return true;
            }
        }
        return false;
    }

    boolean doINeedInitialization() {
        return !isInitiated;
    }

    static List<Integer> getMyChunks() {
        return chunksIHave;
    }

    int getCountOfChunksIHave() {
        return getMyChunks().size();
    }

    public void run(int fileOwnerPort, int myPort, int peerPort) {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("./temp"));
            this.myPort = myPort;
            new ConsumeFileOwner(fileOwnerPort).start();
            new ConsumeFileOwner(peerPort).start();
            new FileMergerThread().start();
            new ServePeer(myPort).start();
        } catch (IOException e) {
        }
    }

    private class FileMergerThread extends Thread {
        public void run() {
            while (doINeedInitialization() || doINeedMoreChunks()) {
                try {
                    Thread.sleep(2000);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            createFileFromChunks(fileName, totalChunkCount);
            System.out.println(fileName + " merged.");
            statistics.entrySet().forEach(entry -> {
                System.out.println("From " + entry.getKey() + " " + entry.getValue() + " chunks.");
            });
        }
    }

    /* ConsumeFileOwner begins */
    private class ConsumeFileOwner extends Thread {
        Socket socket;
        int port;
        int nextChunkID = -1;
        Utility utility = new Utility();

        synchronized public boolean isAlreadyRequested(int nextChunkID) {
            if (alreadyRequestedChunkList.contains(nextChunkID)) {
                return true;
            } else {
                alreadyRequestedChunkList.add(nextChunkID);
                return false;
            }
        }
        synchronized public void removeChunkIDFromRequestedChunkIDList(int nextChunkID) {
            if (alreadyRequestedChunkList.contains(nextChunkID)) {
                alreadyRequestedChunkList.remove(nextChunkID);
            }
        }

        ConsumeFileOwner(int fileOwnerPort) {
            port = fileOwnerPort;
        }

        public void run() {
            System.out.println("Thread to get file from " + port + " started.");
            while (doINeedInitialization() || doINeedMoreChunks()) {
                System.out.println(Thread.currentThread().getName());
                try {
                    // System.out.println("Trying to connect at port "+port + "...");
                    socket = new Socket("localhost", port);
                    System.out.println("Successfully connected to port "+port + ".");
                    String request="", response="";
                    if(doINeedInitialization()){
                        request = myPort+" init";
                        System.out.println("["+myPort + "] REQUESTING for file name and total peer count to ["+port+"] ");
                        utility.sendString(new ObjectOutputStream(socket.getOutputStream()), request);
                        response = utility.receiveString(new ObjectInputStream(socket.getInputStream()));
                        //System.out.println("["+myPort + "]-["+port+"] " + request);
                        //System.out.println("["+port + "]-["+myPort+"] " + response);
                        String[] responseSplitted = response.split(" ");
                        if (Boolean.parseBoolean(responseSplitted[0])) {
                            fileName = responseSplitted[1];
                            totalChunkCount = Integer.parseInt(responseSplitted[2]);
                            isInitiated = true;
                        }
                    } else if (nextChunkID<0){
                        request = myPort+" getlist";
                        System.out.println("[" + myPort + "] REQUESTING chunk id list from " + "[" + port + "]");
                        utility.sendString(new ObjectOutputStream(socket.getOutputStream()), request);
                        response = utility.receiveString(new ObjectInputStream(socket.getInputStream()));
                        //System.out.println("["+myPort + "]-["+port+"] " + request);
                        //System.out.println("["+port + "]-["+myPort+"] " + response);
                        String[] responseSplitted = response.split(" ");
                        if (Boolean.parseBoolean(responseSplitted[0])) {
                            List<Integer> myChunks = getMyChunks();

                            List<Integer> chunksHeHasAndIDont = new ArrayList<Integer>();
                            // System.out.println("MY CHUNKS: " + myChunks.toString());
                            for (int i = 1; i < responseSplitted.length; i++) {
                                if (!myChunks.contains(Integer.parseInt(responseSplitted[i]))) {
                                    chunksHeHasAndIDont.add(Integer.parseInt(responseSplitted[i]));
                                    System.out.print(responseSplitted[i] + " ");
                                }
                            }

                            nextChunkID = chunksHeHasAndIDont.get(new Random().nextInt(chunksHeHasAndIDont.size()));
                            if(isAlreadyRequested(nextChunkID)){
                                nextChunkID = -1;
                            } else {
                                System.out.println(Thread.currentThread().getName());
                                System.out.println(port + " has [" + nextChunkID + "] that I do not have. I will want that now.");
                            }
                        }
                    } else if (nextChunkID>0){
                        request = myPort+" get "+nextChunkID;
                        //System.out.println("["+myPort + "]-["+port+"] " + request);
                        System.out.println("[" + myPort + "] REQUESTING + ["+port+"] for the chunk with id " + nextChunkID);
                        utility.sendString(new ObjectOutputStream(socket.getOutputStream()), request);
                        try {
                            utility.receiveFile(getFilePathByChunkID(nextChunkID), socket.getInputStream());
                            System.out.println("["+ port + "] has given me the chunk [" + nextChunkID + "]");
                            chunksIHave.add(nextChunkID);
                            if (statistics.get(port) == null) {
                                statistics.put(port, 1);
                            } else {
                                statistics.put(port, statistics.get(port) + 1);
                            }
                        } catch (Exception exception) {
                            removeChunkIDFromRequestedChunkIDList(nextChunkID);
                        }
                        nextChunkID = -1;
                    }
                } catch (Exception exception){
                    System.out.println("\t" + exception.getLocalizedMessage() + "\n");
                } finally {
                    try {
                        if (socket != null)
                            socket.close();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }

    }

    /* ConsumeFileOwner ends */
    /* ServePeer begins */
    private static class ServePeer extends Thread {
        int port;
        ServerSocket serverSocket;
        ServePeer(int myPortAsServer) {
            port = myPortAsServer;
        }
        public void run() {
            while (true) {
                try {
                    serverSocket = new ServerSocket(port);
                    System.out.println("Serving at port " + port + ".");
                    try {
                        while (true) {
                            try{
                                new RequestHandler(serverSocket.accept());
                            } catch(Exception exception){
                                try {Thread.sleep(2000);} catch (Exception e) {}
                            }
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                } catch (Exception exception) {
                    System.out.println("Invalid Configuration. Trying again in 2 seconds.");
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {}
                }
                finally {
                    try{serverSocket.close();} catch (Exception exception){exception.printStackTrace();}
                }
            }
        }
    }
    /* ServePeer ends */

    /*RequestHandlerThread begins*/
    private static class RequestHandler extends Thread{
        Socket socket;
        Utility utility = new Utility();
        RequestHandler(Socket socket){
            this.socket = socket;
            try {
                this.handleRequest();
            } catch(Exception exception){}
            finally {
                try {
                    socket.close();
                } catch (Exception exception){}
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
                        response = "false " + fileName + " " + totalChunkCount;
                        System.out.println("RESPOND ["+requesterID+"]  " + response);
                        utility.sendString(new ObjectOutputStream(socket.getOutputStream()), response);
                        break;
                    case "getlist":
                        List<Integer> chunkList = getMyChunks();
                        response = "true";
                        for(int i=0;i<chunkList.size();i++){
                            response += " " + chunkList.get(i);
                        }
                        System.out.println("RESPOND ["+requesterID+"]  with chunk list I have" + response);
                        utility.sendString(new ObjectOutputStream(socket.getOutputStream()), response);
                        break;
                    case "get":
                        String desiredFilePath = getFilePathByChunkID(Integer.parseInt(requestSplitted[2]));
                        System.out.println("RESPOND ["+requesterID+"]  with chunk "+ requestSplitted[2] + " from path " + desiredFilePath);
                        utility.sendFile(desiredFilePath, socket.getOutputStream());
                        break;
                    case "thanks":
                        break;
                }
            } catch (Exception exception){exception.printStackTrace();}
        }
    }
    /*RequestHandlerThread ends*/
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
    public String receiveString(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        return (String) objectInputStream.readObject();
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

    public void receiveFile(String filePath, InputStream inputStream) throws Exception {
        try {
            File someFile = new File(filePath);
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
            throw exception;
        } finally {
            try {
                if(inputStream != null) {inputStream.close();}
            } catch (Exception exception) {
                System.out.println(exception.getLocalizedMessage() + "\n");
                throw exception;
            }
        }
    }
}
/* Utility ends */