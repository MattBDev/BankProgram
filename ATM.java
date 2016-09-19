import java.net.*;
import java.io.*;

public class ATM {
    public static void main(String[] args) {
        try {
            InetAddress address = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);
            Socket router = new Socket(address, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(router.getInputStream()));
            String incomingTxt = null;
            while ((incomingTxt = in.readLine()) != null) {
                System.out.println("Bank: " + incomingTxt);
            }
        } catch (Exception ex) {
        }
    }
}
