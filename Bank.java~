import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.nio.channels.*;

//TODO: CHECK ENVIRONMENT, consider path variable
//TODO: impliment data transfer verification between bank and ATM; assume faulty router; can data be verified? Yes!
/*
B > msg1a > R > msg1b > A
B < msg? < R < msg1b < A
*/

public class Bank {
	
	//TODO: put in real help message
	public static void hlpMsg() {
		System.out.println("port_dir, port_router, ip_router");
	}
	
	//Run with three arguments: port_dir	port_router		ip_router
	public static void main (String args[]) throws IOException {
		
		Semaphore sem = new Semaphore(1, true);
		int port_dir = 0;
		int port_rout = 0;
		String ip;
		//ServerSocket ss;
		BankAccess dir;
		BankAccess atm;
		ServerSocketChannel ssc;
		SocketChannel sc = null;
		
		if (args.length < 3 | args[0].equals("--help")) {
			hlpMsg();
			return;
		}
		
		try {
				port_dir = Integer.parseInt(args[0]);
				port_rout = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
				hlpMsg();
				return;
		}
		
		//Also: need to manage synchronization issues
		ip = args[2];
		//Do much better handling for malicious behavior; timeouts, etc.
		//atm = new BankAccess(new Socket(ip, port_rout), false, sem);
		//atm.start();
		//ss = new ServerSocket(port_dir);
		try {
			ssc = ServerSocketChannel.open();
			ssc.socket().bind(new InetSocketAddress(4321));
			sc = ssc.accept();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dir = new BankAccess(sc, true, sem);		
		dir.start();
		
	}
}
