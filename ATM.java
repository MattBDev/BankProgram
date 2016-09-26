import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class ATM {
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
        atm.run();
    }
    public void write(String msg) {
        System.out.println("Writing: " + msg);
		char[] msg2 = msg.toCharArray();
		System.out.println("total: " + String.valueOf(msg2));
		System.out.println("end: " + String.valueOf(msg2[msg2.length-1]));
		char cr = 13;
		char lf = 10;
		msg += (String.valueOf(cr) + String.valueOf(lf));
        byte[] buff = msg.getBytes();
        ByteBuffer buf = ByteBuffer.wrap(buff);
        try {
            socketChannel.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/*
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
	*/
	
	private String read(long timeout) throws IOException {
		byte[] buff = new byte[128];
		ByteBuffer buf = ByteBuffer.wrap(buff);
		String in = null;
		int c;
		long time = System.currentTimeMillis();
		long delta = 0;
		int off = 0;
		boolean overflow = false;
		
		//insert artificial delay; hinders attackers, but is not disruptive to
		//normal users
		
		try {

			//TODO: guarantee use of PuTTY in passive mode by beginning negotiations and throwing error otherwise
			
			//NOTE: this communication relies on the telnet default line-oriented communications protocol (in nothing else about the protocol).
			//At beginning of read, assume the transmitter is benign and informed. Failing these assumptions, disconnect.
			//Once begun, take in data until a 1310 is read. Then return.
			//If enough time passes without a 1310, timeout.		
			while ((c = socketChannel.read(buf)) != -1) {
				off = buf.position();
				//System.out.println(new String(buff) + ", " + off);
				for (int i = 0; i < 10; i++) {
					int b = (int)buff[i];
					//System.out.print(b);
				}
				//System.out.println("\n done");

				
				//TODO: be sure that this is standard on all systems
				if (off > 1 && ((byte)buff[off-2]) == 13 && ((byte)buff[off-1]) == 10) {
					if (overflow) {
						return null;
					}
					in = new String(Arrays.copyOfRange(buff, 0, off - 2));
					return in;
				}

				if (buf.remaining() == 0) {
					buf.rewind();
					overflow = true;
				}

				/*
				delta = System.currentTimeMillis() - time;
				//System.out.println(delta);
				if (delta > timeout && timeout > 0) {
					throw new IOException();
				}
				*/

			}
			
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Running. Enter loop");           
			while (true) {
				System.out.println(read(0));
				write(reader.readLine());
			}
        } catch (Exception ex) {
            ex.getStackTrace();
        }
    }
}
