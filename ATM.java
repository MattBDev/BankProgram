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
    private boolean connected = false;

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
        byte[] buff = msg.getBytes();
        ByteBuffer buf = ByteBuffer.wrap(buff);
        try {
            socketChannel.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readWrite(String msg) throws IOException {
        byte[] buff = new byte[128];
        ByteBuffer buf = ByteBuffer.wrap(buff);
        String in = null;
        int c;
        int off = 0;
        boolean overflow = false;

        try {

            while ((c = socketChannel.read(buf)) != -1) {
                off = buf.position();

                if (off > 1 && ((byte) buff[off - 2]) == 13 && ((byte) buff[off - 1]) == 10) {
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


            }

            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String read() throws IOException {
        byte[] buff = new byte[128];
        ByteBuffer buf = ByteBuffer.wrap(buff);
        String in = null;
        int c;
        int off = 0;
        boolean overflow = false;

        try {

            while ((c = socketChannel.read(buf)) != -1) {
                off = buf.position();

                if (off > 1 && ((byte) buff[off - 2]) == 13 && ((byte) buff[off - 1]) == 10) {
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

            }

            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void runWrite() {
        new Thread(new Runnable() {
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
    }

    public void runRead() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String in;
                while (connected) {
                    try {
                        in = ATM.this.read();
                        if (in != null && in.equalsIgnoreCase("BankException")) {
                            System.out.println(in);
                            System.out.println(ATM.this.read());
                            break;
                        }
                        System.out.println(in);
                    } catch (Exception ex) {
                    }
                }
                try {
                    socketChannel.close();
                } catch (IOException e) {
                }
                connected = false;
                System.out.println("Disconnected");

            }
        }).start();
    }
}
