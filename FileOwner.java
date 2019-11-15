import java.net.*;
import java.io.*;
import java.util.*;

public class FileOwner {

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
            return "";
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

    public static void initialize(int port){
        controlPort = port;
        dataPort = port+1;
        try {
            controlServerSocket = new ServerSocket(controlPort);
            dataServerSocket = new ServerSocket(dataPort);

            try {
				while (true) {
					new ControlSocketThreadHandler(controlServerSocket.accept()).start();
				}
			} finally {
				controlServerSocket.close();
				dataServerSocket.close();
			}
        } catch(Exception exception) {
            System.out.println("The server failed to start.");
        }
    }

    public static boolean isArgumentsValid(String[] arguments){
        //if(arguments.length!=1 && )
        return true;
    }

    public static void main(String[] arguments){
        if(isArgumentsValid(arguments)){
            initialize(Integer.parseInt(arguments[0]));
        } else {
            System.exit(0);
        }
    }

    /* FileOwner Class implementation is above */
    /* ControlSocketThreadHandler Class implementation is below */
    private static class ControlSocketThreadHandler extends Thread {
		private Socket connection;
		private ObjectInputStream in;
        private ObjectOutputStream out;
        
        Utility utility = new Utility();

		public ControlSocketThreadHandler(Socket connection) {
			this.connection = connection;
		}

		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				try {
					while (true) {
						String message = (String) in.readObject();

					}
				} catch (Exception exception) {
					System.err.println(exception.getLocalizedMessage());
				}
			} catch (IOException ioException) {
				System.err.println(ioException.getLocalizedMessage());
			} finally {
				try {
					in.close();
					out.close();
					connection.close();
				} catch (IOException ioException) {
				}
			}
        }
        
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
    
    
}

