import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.nio.channels.*;


public class Bank {
	
	public static void hlpMsg() {
		System.out.println("run with arguments: port_dir, port_router, ip_router");
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
		ServerSocketChannel ssc = null;
		SocketChannel atm_sc = null;
		SocketChannel dir_sc = null;
		boolean atm_online = false;
		boolean dir_online = false;
		
		if (args.length < 3 || args[0].equals("--help")) {
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
		
		ip = args[2];
		try {
			ssc = ServerSocketChannel.open();
			ssc.socket().bind(new InetSocketAddress(port_dir));
			ssc.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (true) {
			if (!atm_online) {
				try {
					atm_sc = SocketChannel.open();
					atm_sc.connect(new InetSocketAddress(ip, port_rout));
					while (!atm_sc.finishConnect()) {
						
					}
					atm_online = true;
					atm = new BankAccess(atm_sc, false, sem);
					atm.start();

				} catch (IOException e) {
					atm_online = false;
				}
			}

			if (!dir_online) {
				try {
					dir_sc = ssc.accept();
					if (dir_sc != null && dir_sc.isConnected()) {
						dir_online = true;
						dir = new BankAccess(dir_sc, true, sem);
						dir.start();
					}
				} catch (IOException e) {
					dir_online = false;
				}
			}
		}
		
	}
}
