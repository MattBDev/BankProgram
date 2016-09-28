import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class BankAccess implements Runnable {
	
	private boolean dir;
	private Thread t;
	private Semaphore control;
	private static final String accounts[] = {"Jason", "Matthew"};
	private SocketChannel sc;

    private class Account {

        private MoneyFloat bal;
        private int pin;
        private String name;
		
        protected Account(float bal, int pin, String name) throws BankException {
			this.bal = new MoneyFloat(bal);
            this.pin = pin;
            this.name = name;
        }

        protected void deposit(float amt) throws BankException {
            MoneyFloat a = new MoneyFloat(amt);
            if (a.isPositive()) {
                bal = bal.sum(a);
            } else {
                throw new BankException("Deposits must be greater than zero");
            }
        }

        protected Float withdraw(float amt) throws BankException {
            MoneyFloat a = new MoneyFloat(amt);
            if (a.isPositive()) {
                if (bal.compare(a) >= 0) {
                    bal = bal.sum(a.flip());
					return -a.num;
                } else {
                    throw new BankException("Insufficient funds.");
                }
            } else {
                throw new BankException("Deposits must be greater than zero");
            }

        }
		
        protected void checkPin(int p) throws BankException {
            if (p != pin) {
				throw new BankException("Incorrect PIN");
            }
        }

        protected String getName() {
            return name;
        }

        protected int getPin() {
            return pin;
        }

        protected float getBal() {
            return bal.num;
        }

    }

    private class MoneyFloat {

        public Float num;

        public MoneyFloat(Float f) throws BankException {
            num = f;
            validate();
        }

        public void validate() throws BankException {
            num = new Float(String.format(java.util.Locale.US, "%.2f", num));

            if (num >= 1000000) {
                throw new BankException("No monetary amount may exist greater than one million.");
            }
        }

        public MoneyFloat flip() throws BankException {
            num *= -1;
            validate();
            return this;
        }

        public MoneyFloat sum(MoneyFloat f) throws BankException {
            MoneyFloat r = new MoneyFloat(num + f.num);
            if (f.isPositive()) {
                if (num >= r.num) {
                    throw new BankException("Error: funds too large to deposit this amount.");
                }
            } else {
                if (num <= r.num) {
                    throw new BankException("Error: withdraw request too large.");
                }
            }
            return r;
        }

        public int compare(MoneyFloat f) {
            if (f.num < num) {
                return 1;
            } else if (f.num > num) {
                return -1;
            }
            return 0;
        }

        public boolean isPositive() {
            return num > 0;
        }

    }

    private class BankException extends Exception {
    	private String raw;
        public BankException(String msg) {
            super("BankException" + String.valueOf((char)13) + String.valueOf((char)10) + msg);
            raw = msg;
        }
        public String rawMsg() {
        	return raw;
        }
    }


	public BankAccess(SocketChannel s, boolean direct, Semaphore sem) throws IOException {
		dir = direct;
		control = sem;
		sc = s;
		sc.configureBlocking(false);
	}
	
	public void start() {
		t = new Thread(this);
		t.start();
	}

	//TODO: handle malicious disconnects
	@Override
	public void run() {
		boolean connected = false;
		while (true) {
			try {
				if (read(0).equals("connected")) {
					connected = true;
				}
			} catch (BankException | TimeoutException e) {
				//TODO: WHYYYY
				write(e.getMessage());
				System.out.println("ohshit");
				break;
			}
			while (connected) {
				System.out.println("ATM connected. Begin communication");
				write("hello");
				String in;
				try {
					in = read(20 * 1000);
				} catch (BankException | TimeoutException e) {
					write(e.getMessage());
					break;
				}
				String cmd[] = in.split("\\s+");
				Account acct = null;

				if (cmd.length > 1) {
					try {
						acct = buildAccount(cmd[1]);
					} catch (BankException e) {
						write(e.getMessage());
						if (e.rawMsg().equals("Timeout")) {
							System.out.println("ATM timed out");
							break;
						}
					}
				} else {
					write("Input command is not in a recognized form. Commands require at least two arguments");
				}
		
			
				if (acct != null) {
					try {
						parseCommand(cmd, acct);
					} catch (BankException | TimeoutException e) {
						write(e.getMessage());
					}
				} else {
						write("Account not found");
				}
	
			}
			connected = false;
		}
	}
	
	private void parseCommand(String[] cmd, Account acct) throws BankException, TimeoutException {
		switch (cmd[0]) {
			case "getBalance":
				getBal(cmd, acct);
				break;
			case "deposit":
				deposit(cmd, acct);
				break;
			case "withdraw":
				withdraw(cmd, acct);
				break;
			default:
				throw new BankException("Command not recognized. Recognized commands are \"getBalance\", \"deposit\", and \"withdraw\"");
		}
		updateAccount(acct);
	}
	
	private <T extends Number> T parseLine(String[] in, String match) throws BankException {
		String error = "Error: Account file formatted incorrectly";
		if (in.length == 2) {
			try {
				switch (in[0]) {
					case "pin:":
						return (T)Integer.valueOf(in[1]);
					case "bal:":
						return (T)Float.valueOf(in[1]);
				}
			} catch (NumberFormatException e) {
				throw new BankException(error + ". PIN or Balance is in the wrong format.");
			}
		}
		throw new BankException(error);
	}
	
	private Account buildAccount(String name) throws BankException {
		
		String s_bal[];
		String s_pin[];
		
		try {
			if (!name.equals(accounts[0]) && !name.equals(accounts[1])) {
				throw new BankException("Account name not recognized");
			}

			try (BufferedReader in = new BufferedReader(new FileReader((name + ".acct")))){
				control.acquire();

				s_pin = in.readLine().split("\\s+");
				s_bal = in.readLine().split("\\s+");
				
				control.release();
			
				if (s_bal != null && s_pin != null) {
					
					float bal = this.<Float>parseLine(s_bal, "bal");
					int pin = this.<Integer>parseLine(s_pin, "pin");
									
					return new Account(bal, pin, name);
					
				} else {
					throw new BankException("Error: Account file formatted incorrectly");
				}
			} catch (InterruptedException e) {
                control.release();
				e.printStackTrace();
				throw new BankException("Error: Process Interrupted");
			}
						
		} catch (IOException e) {
			control.release();
			throw new BankException("Error: Account file not found");
		}

	}
	

	private void updateAccount(Account acct) throws BankException {

		try {
			if (!acct.name.equals(accounts[0]) && !acct.name.equals(accounts[1])) {
				throw new BankException("Account name not recognized");
			}
			
			try (BufferedWriter out = new BufferedWriter(new FileWriter((acct.getName() + ".acct")))) {
				control.acquire();

				out.write("pin: " + String.valueOf(acct.getPin()));
				out.newLine();
				out.flush();
				out.write("bal: " + String.valueOf(acct.getBal()));
				out.flush();
				
				control.release();
			} catch (InterruptedException e) {
                control.release();
				e.printStackTrace();
				throw new BankException("Error: Process interrupted");
			}
			
		} catch (IOException e) {
			control.release();
			throw new BankException("Error: Account file not found");
		}
	}
	
	private void getBal(String cmd[], Account acct) throws BankException, TimeoutException {
		checkPin(acct);
		write(String.valueOf(acct.getBal()));
	}
	
	private void deposit(String cmd[], Account acct) throws BankException {
		if (dir) {
			if (cmd.length == 3) {
				float amt = parseAmt(cmd[2]);
				acct.deposit(amt);
				write(String.valueOf(acct.getBal()));
			} else {
				throw new BankException("Invalid number of arguments. Two arguments (account, amount) are used by this command.");
			}
		} else {
			throw new BankException("Deposits can only be made via a direct connection.");
		}
	}
	
	private void withdraw(String cmd[], Account acct) throws BankException, TimeoutException {
		if (cmd.length == 3) {
			float amt = parseAmt(cmd[2]);
			checkPin(acct);
			write(("Here are $" + acct.withdraw(amt) + " dollars."));
			write(String.valueOf(acct.getBal()));
		} else {
			throw new BankException("Invalid number of arguments. Two arguments (account, amount) are used by this command.");
		}
	}
	
	private float parseAmt(String amt) throws BankException {
		try {
			return Float.parseFloat(amt);
		} catch (NumberFormatException e){
			throw new BankException("Invalid amount; enter only integers");
		}
	}
	
	private void checkPin(Account acct) throws BankException, TimeoutException {
        write("pin?");
        int pin_in = 0;
        String in = read(20000);

        try {
			Thread.sleep(4*1000);
		} catch (InterruptedException e) {
			throw new BankException("Process interrupted");
		}
		
		if (in.length() != 4) {
			throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
		}
		try {
			pin_in = Integer.parseInt(in);
		} catch (NumberFormatException e) {
			throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
		}
		acct.checkPin(pin_in);
	}
	
	private void write(String msg) {
		char cr = 13;
		char fl = 10;
		msg += String.valueOf(cr);
		msg += String.valueOf(fl);
		byte[] buff = msg.getBytes(StandardCharsets.US_ASCII);
		ByteBuffer buf = ByteBuffer.wrap(buff);
		try {
			sc.write(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String read(long timeout) throws BankException, TimeoutException {
		byte[] buff = new byte[128];
		ByteBuffer buf = ByteBuffer.wrap(buff);
		String in;
		long start = System.currentTimeMillis();
		long delta;
		int off;
		boolean overflow = false;
		
		try {
			t.sleep(2*1000);
		} catch (InterruptedException e) {
			throw new BankException("Process interrupted");
		}
		
		try {
	
			while (sc.read(buf) != -1) {
				off = buf.position();

				if (off > 1 && buff[off-2] == 13 && buff[off-1] == 10) {
					if (overflow) {
						throw new BankException("Your input is garbage and so are you.");
					}					
					in = new String(Arrays.copyOfRange(buff, 0, off - 2));
					return in;
				}

				if (buf.remaining() == 0) {
					buf.rewind();
					overflow = true;
				}

				delta = System.currentTimeMillis() - start;
				if (delta >= timeout && timeout > 0) {
					throw new TimeoutException();
				}

			}
			
			return "";
		} catch (IOException e) {
			throw new BankException(e.getMessage());
		}
	}

}
