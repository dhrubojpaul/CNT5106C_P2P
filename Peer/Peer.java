public class Peer {
    public static boolean isArgumentsValid(String[] arguments){
        return arguments.length == 3;
    }
    public static void main(String[] arguments){
        if(isArgumentsValid(arguments)){
            PeerHandler peerHandler = new PeerHandler();
            peerHandler.run(Integer.parseInt(arguments[0]), 
                            Integer.parseInt(arguments[1]),
                            Integer.parseInt(arguments[2]));
        } else {
            System.exit(0);
        }
    }
}