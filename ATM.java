import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ATM {
    private SocketChannel socketChannel = null;
    private Thread thread;
    private boolean connected = false;
    private Thread r;
    private Thread w;

    public ATM(InetAddress address, int port) {
        while (!connected) {
            try {
                socketChannel = SocketChannel.open();
                socketChannel.connect(new InetSocketAddress(address, port));
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
        write("connected");
    }

    public void write(String msg) {
        char cr = 13;
        char lf = 10;
        msg += (String.valueOf(cr) + String.valueOf(lf));
        byte[] buff = msg.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buf = ByteBuffer.wrap(buff);
        try {
            socketChannel.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String read() throws IOException {
        byte[] buff = new byte[128];
        ByteBuffer buf = ByteBuffer.wrap(buff);
        String in;
        int off;
        boolean overflow = false;

        try {

            while (socketChannel.read(buf) != -1) {
                off = buf.position();

                if (off > 1 && ((byte) buff[off - 2]) == 13 && ((byte) buff[off - 1]) == 10) {
                    if (overflow) {
                        return "Reading failed with overflow error";
                    }
                    in = new String(Arrays.copyOfRange(buff, 0, off - 2));
                    return in;
                }

                if (buf.remaining() == 0) {
                    buf.rewind();
                    overflow = true;
                }

            }

            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "Encountered IOException while reading";
        }
    }

    public void runWrite() {
        w = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    while (connected) {
                        write(reader.readLine());
                    }
                } catch (Exception ex) {
                    ex.getStackTrace();
                }

            }
        });
        w.start();
        try { r.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        
    }

    public void runRead() {
        r = new Thread(new Runnable() {
            @Override
            public void run() {
                String in;
                while (connected) {
                    try {
                        in = ATM.this.read();
                        String[] split = in.split(String.valueOf((char) 13) + String.valueOf((char) 10));
                        for (String s : split) {
                            System.out.println(s);
                        }
                        if (in.contains("TimeoutException")) {
                            connected = false;
                            socketChannel.close();
                        }
                    } catch (IOException ex) {
                        ex.getMessage();
                    }
                }
                System.out.println("Disconnected");
            }
        });
        r.start();
        try {
        	r.join();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
