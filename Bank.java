import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.nio.channels.*;

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
			System.out.println("test2");
			ssc = ServerSocketChannel.open();
			ssc.socket().bind(new InetSocketAddress(4321));
			System.out.println("Try accepting!");
			sc = ssc.accept();
			System.out.println("Stop accepting!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		dir = new BankAccess(sc, true, sem);		
		//dir = new BankAccess(ss.accept(), true, sem);
		dir.start();
		
	}
}
