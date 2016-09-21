import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.Arrays;
import java.nio.channels.*;

public class BankAccess implements Runnable {
	
	boolean dir;
	String NEW_LINE;
	boolean read;
	
	BufferedReader br;
	BufferedWriter bw;
	
	InputStreamReader ir;
	OutputStreamWriter ow;
	private Semaphore control;
	

	private class Account {
		
		private MoneyFloat bal;
		private int pin;
		private String name;
		int tries;
		
		public Account(float bal, int pin, String name) throws BankException {
			read = false;
			this.bal = new MoneyFloat(bal);
			this.pin = pin;
			this.name = name;
			tries = 0;
		}
		
		public void deposit(float amt) throws BankException {
			System.out.println("amt: " + amt);
			MoneyFloat a = new MoneyFloat(amt);
			System.out.println("after: " + a.num + ", pos: " + a.isPositive());
			if (a.isPositive()) {
				bal = bal.sum(a);
			} else {
				//This is being called erroneously. Why?
				throw new BankException("Deposits must be greater than zero");
			}
		}
		
		//throw withdraw error: wrong pin, or invalid amount
		public void withdraw(float amt) throws BankException {
			MoneyFloat a = new MoneyFloat(amt);
			if (a.isPositive()) {
				if (bal.compare(a) >= 0) {
					bal = bal.sum(a.flip());
				} else {
					throw new BankException("Insufficient funds.");
				}
			} else {
				throw new BankException("Deposits must be greater than zero");
			}

		}
		
		public void checkPin(int p) throws BankException {
			if (tries < 6) {
				if (p == pin) {
					tries = 0;
				} else {
					tries++;
					throw new BankException("Invalid PIN: " + (6-tries) + " tries remaining.");
				}
			} else {
				throw new BankException("Too many tries. Account has been locked.");
			}
		}

		public String getName() {
			return name;
		}
		
		//TODO: maybe remove this method
		public int getPin() {
			return pin;
		}
		
		public float getBal() {
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
			System.out.println("Added: " + r.num);
			if (f.isPositive()) {
				if (num >= r.num) {
					throw new BankException("Error: funds too large to deposit this amount.");
				}
			} else {
				System.out.println("Before: " + num);
				System.out.println("After: " + r.num);
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
		public BankException(String msg) {
			super(msg);
		}
	}
	

	public BankAccess(SocketChannel s, boolean direct, Semaphore sem) throws IOException {
		dir = direct;
		control = sem;

		//br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		//bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		
		//ir = new InputStreamReader(s.getInputStream());
		//ow = new OutputStreamWriter(s.getOutputStream());
		ir=null;
		ow=null;
	}
	
	public void start() {
		(new Thread(this)).start();
	}

	@Override
	public void run() {
		while (true) {
			write("hello");
			String in = null;
			try {
				in = read(0);
			} catch (BankException e) {
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
				}
			} else {
				write("Input command is not in a recognized form. Commands require at least two arguments");
			}
			
			
			if (acct != null) {
				try {
					parseCommand(cmd, acct);
				} catch (BankException e) {
					write(e.getMessage());
				}
			} else {
				write("Account not found");
			}
		
		}
				
	}
	
	private void parseCommand(String[] cmd, Account acct) throws BankException {
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
						T i = (T)Integer.valueOf(in[1]);
						return i;
					case "bal:":
						T f = (T)Float.valueOf(in[1]);
						return f;
				}
			} catch (NumberFormatException e) {
				throw new BankException(error + ". PIN or Balance is in the wrong format.");
			}
		}
		throw new BankException(error);
	}
	
	private Account buildAccount(String name) throws BankException {
		
		BufferedReader in = null;
		String s_bal[];
		String s_pin[];
		
		try {
			//TODO: be more careful about what files are opened
			//(Attempt to) prevent them from opening any other potentially malicious files
			if (!(name.equals("Jason") || name.equals("Matthew"))) {
				throw new BankException("Account name not recognized");
			}
			//TODO: handle this better; consider catch block and what to do.
			try {
				control.acquire();
				//TODO: Consider doing this differently. Ie, not BufferedReader
				in = new BufferedReader(new FileReader((name + ".acct")));

				s_pin = in.readLine().split("\\s+");
				s_bal = in.readLine().split("\\s+");
				
				in.close();
				control.release();
			
				if (s_bal != null && s_pin != null) {
					
					float bal = this.<Float>parseLine(s_bal, "bal");
					int pin = this.<Integer>parseLine(s_pin, "pin");
									
					return new Account(bal, pin, name);
					
				} else {
					throw new BankException("Error: Account file formatted incorrectly");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new BankException("Error: Account file not found");
			}
						
		} catch (IOException e) {
			throw new BankException("Error: Account file not found");
		}
/*
		finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			control.release();
		}
*/
	}
	
	//TODO: make this more secure.
	private void updateAccount(Account acct) throws BankException {
		
		BufferedWriter out = null;
		String s_bal[];
		String s_pin[];
		
		try {
			//prevent them from opening any other potentially malicious files
			if (!(acct.getName().equals("Jason") || acct.getName().equals("Matthew"))) {
				throw new BankException("Account name not recognized");
			}
			
			try {
				control.acquire();
				out = new BufferedWriter(new FileWriter((acct.getName() + ".acct")));
				
				out.write("pin: " + String.valueOf(acct.getPin()));
				out.newLine();
				out.flush();
				out.write("bal: " + String.valueOf(acct.getBal()));
				out.flush();
				
				out.close();
				control.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new BankException("Error: Account file not found");
			}
			
		} catch (IOException e) {
			throw new BankException("Error: Account file not found");
		}
	}
	
	private void getBal(String cmd[], Account acct) throws BankException {
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
	
	private void withdraw(String cmd[], Account acct) throws BankException {
		if (cmd.length == 3) {
			float amt = parseAmt(cmd[2]);
			checkPin(acct);
			acct.withdraw(amt);
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
	
	private boolean checkPin(Account acct) throws BankException {
		String in;
		int pin_in = 0;
		int tries = 0;
		write("pin?");
		in = read(20000);
		if (in.length() != 4) {
			throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
		}
		try {
			pin_in = Integer.parseInt(in);
		} catch (NumberFormatException e) {
			throw new BankException("Invalid Format: Enter a four digit (0-9) integer");
		}
		acct.checkPin(pin_in);
		return true;
	}
	
	//TODO: handle this better
	private void write(String msg) {
		char cr = 13;
		char fl = 10;
		msg += String.valueOf(cr);
		msg += String.valueOf(fl);
		System.out.println("Writing: " + msg);
		char[] buff = msg.toCharArray();
		try {
			ow.write(buff, 0, buff.length);
			ow.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//TODO: handle this better
	private String read(long timeout) throws BankException {
		int len = 24;
		int off = 0;
		char[] buff = new char[100];
		String in = null;
		int c;
		long time = System.currentTimeMillis();
		long delta = 0;
		
		try {
			while (!ir.ready()) {
				delta = System.currentTimeMillis() - time;
				System.out.println(delta);
				if (delta > timeout && timeout > 0) {
					throw new BankException("Took too long to respond.");
				}
			}
			
			//TODO: is this vulnerable? Can I be tricked into hanging at
			// ir.read() indefinitely? What if the packets bounce around
			// forever, and never reach me? What if no newline ever arrives?
			while ((c = ir.read(buff, off, len)) > 0) {
				
				off += c;
				
				if (((byte)buff[off-2]) == 13 && ((byte)buff[off-1]) == 10) {
					in = new String(Arrays.copyOfRange(buff, 0, off - 2));
					//System.out.println("Final string: " + in);
					return in;
				}
				
				//If input is too large, discard and everything else until
				//a newline is received. Do this by setting offset to 0,
				//overwriting buffer until garbage command is terminated.
				if (off > 80) {
					off = 0;
					System.out.println("input too large for buffer. Stopping read.");
				}
				
				while (!ir.ready()) {
					delta = System.currentTimeMillis() - time;
					System.out.println(delta);
					if (delta > timeout && timeout > 0) {
						throw new BankException("Took too long between packets.");
					}
				}
				
			}
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
