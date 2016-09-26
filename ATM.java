import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ATM implements Runnable{
    private SocketChannel socketChannel = null;
    private Thread thread;

    public ATM(InetAddress address, int port) {
	boolean connected = false;	
	while (!connected) {
		try {
		    socketChannel = SocketChannel.open();
		    socketChannel.connect(new InetSocketAddress(address,port));
		    if (socketChannel.finishConnect()) {
			connected = true;
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	try {
	    socketChannel.configureBlocking(false);
	} catch (IOException e) {
	    e.printStackTrace();
	}
        this.start();
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }
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
        new Thread(atm).start();
    }
    public void write(String msg) {
        System.out.println("Writing: " + msg);
        byte[] buff = msg.getBytes();
        ByteBuffer buf = ByteBuffer.wrap(buff);
        try {
            socketChannel.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read() {
        byte[] buff = new byte[256];
        ByteBuffer buf = ByteBuffer.wrap(buff);
        try {
            while (socketChannel.read(buf) != -1) {
                String s = new String(buf.array());
                buf.rewind();
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	    System.out.println("Running. Enter loop");            
	    while (true) {
		System.out.println("In loop!");
		read();
		write(reader.readLine());
            }
        } catch (Exception ex) {
            ex.getStackTrace();
        }
    }
}
