public class Peer {
    public static boolean isArgumentsValid(String[] arguments){
        if (arguments.length == 3){
            try {
                Integer.valueOf(arguments[0]);
                Integer.valueOf(arguments[1]);
                Integer.valueOf(arguments[2]);
                return true;
            } catch (Exception exception){
                System.out.println("Invalid Input");
                System.exit(0);
            }
        } else {
            System.out.println("Invalid Input");
            System.exit(0);
        }
        return false;
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