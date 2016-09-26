import java.net.InetAddress;

public class ATMLauncher {
    public static void main(String[] args) {
        InetAddress address = null;
        int port = 0;
        try {
            address = InetAddress.getByName(args[0]);
            port = Integer.parseInt(args[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ATM atm = new ATM(address, port);
        atm.run();
    }
}
